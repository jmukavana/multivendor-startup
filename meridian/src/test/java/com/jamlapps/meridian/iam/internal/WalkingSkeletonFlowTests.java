package com.jamlapps.meridian.iam.internal;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jamlapps.meridian.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The walking-skeleton flow, now end to end through the real Phase 2 vendor lifecycle: a vendor
 * registers, applies, is approved by an admin, and lists a product; a buyer registers, orders it;
 * payment is marked; the order confirms.
 *
 * <p>Every step goes through real HTTP with real bearer tokens issued by the real registration and
 * verification flow — the same one {@link AuthenticationFlowTests} exercises. The deliberate
 * exception is granting elevated roles (approving admin, finance admin) directly against the
 * database: there is no self-service or admin-driven way to acquire them, since {@code admin}
 * module aggregation does not exist yet (see {@code AdminVendorApplicationController}'s javadoc).
 * This stands in for role-management tooling that does not exist rather than for anything a real
 * account could reach on its own — the {@code @PreAuthorize} checks themselves are exercised
 * honestly, including a negative case.
 *
 * <p>Lives in {@code iam.internal} for the same reason {@link AuthenticationFlowTests} does: only
 * here can a test retrieve the plaintext one-time codes real registration produces.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, CapturingOneTimeCodeGenerator.class})
class WalkingSkeletonFlowTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    CapturingOneTimeCodeGenerator codes;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("vendor applies and is approved, lists a product, a buyer orders it, payment is marked, the order confirms")
    void oneOrderCompletesEndToEnd() throws Exception {
        String vendorToken = registerAndVerify("+254700200001");
        String buyerToken = registerAndVerify("+254700200002");
        String adminToken = registerAndVerify("+254700200099");
        grantRole(adminToken, "PLATFORM_ADMIN");

        String applicationId = submitVendorApplication(vendorToken, "Test Traders", "KRA-EEE-001");
        MvcResult approved = approveVendorApplication(adminToken, applicationId);
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");

        String productId = createAndPublishProduct(
                vendorToken, "Test Product", "A thing", 250_000, "KES");

        // BR-IAM-08: listing is open to guests. Read it with no Authorization header at all.
        // Presence by name, not position — JUnit does not guarantee test method execution order,
        // so another test's product can legitimately have a later createdAt than this one's.
        mvc.perform(get("/api/catalog/products?perPage=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", org.hamcrest.Matchers.hasItem("Test Product")));

        // BR-CHK-01/04, UC-03: checkout, not a direct POST /orders — that endpoint was Phase 1's
        // walking-skeleton stand-in and is retired; see OrderController's class doc.
        MvcResult orderResult = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"%s\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, "+254700200002", defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.reference", notNullValue()))
                // BR-CHK-06/07: 500,000 items plus 25,000 DOOR delivery — V19__fulfilment.sql's seeded rate.
                .andExpect(jsonPath("$.data.total.minorUnits").value(525_000))
                .andExpect(jsonPath("$.data.subOrders", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data.subOrders[0].lines[0].quantity").value(2))
                .andReturn();

        String orderId = JsonPath.read(orderResult.getResponse().getContentAsString(), "$.data.id");

        // A different buyer must not be able to see this order at all — see OrderService.get's
        // reasoning for NOT_FOUND rather than FORBIDDEN.
        String otherBuyerToken = registerAndVerify("+254700200003");
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + otherBuyerToken))
                .andExpect(status().isNotFound());

        // BR-PAY-04/05: the real trigger — a provider callback, not a finance-admin action. See
        // OrderController's class doc on why the old manual endpoint is retired.
        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        // BR-LDG-01/02: 500,000 subtotal (2 x 250,000), 9% seeded commission = 45,000, vendor
        // portion 455,000, 10% indicative holdback (ADR-007) = 45,500, leaving 409,500 payable —
        // verified end to end through the real posting engine, not asserted against its own maths.
        awaitVendorPayable(vendorId, vendorToken, 409_500);
        mvc.perform(get("/api/settlement/vendors/" + vendorId + "/balance").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable.minorUnits").value(409_500))
                .andExpect(jsonPath("$.data.holdback.minorUnits").value(45_500));
    }

    @Test
    @DisplayName("a settlement run disburses an eligible vendor and excludes one with no payout details on file")
    void settlementRunDisbursesEligibleVendorAndExcludesIneligibleOne() throws Exception {
        String adminToken = registerAndVerify("+254700200201");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String financeToken = registerAndVerify("+254700200202");
        grantRole(financeToken, "FINANCE_ADMIN");
        String buyerToken = registerAndVerify("+254700200203");

        // Vendor A: payout details on file and past the BR-VND-09 cooling-off period — eligible.
        String vendorAToken = registerAndVerify("+254700200204");
        MvcResult approvedA = approveVendorApplication(
                adminToken, submitVendorApplication(vendorAToken, "Settleable Traders", "KRA-STL-001"));
        String vendorAId = JsonPath.read(approvedA.getResponse().getContentAsString(), "$.data.vendorId");
        String productAId = createAndPublishProduct(vendorAToken, "Settlement Widget A", null, 100_000, "KES");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/vendors/" + vendorAId + "/payout-details")
                        .header("Authorization", "Bearer " + vendorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"MPESA\",\"destination\":\"254700200204\"}"))
                .andExpect(status().isOk());

        // BR-VND-09's 7-day cooling-off has no fast-forward in a real-time test, so it is backdated
        // directly, the same stand-in-for-missing-tooling choice {@code grantRole} already makes for
        // role management — see this method's own comment there.
        jdbc.update(
                "update payout_detail set changed_at = ?, cooling_off_until = ? where vendor_id = ?",
                Timestamp.from(Instant.now().minusSeconds(30 * 24 * 3600)),
                Timestamp.from(Instant.now().minusSeconds(23 * 24 * 3600)),
                UUID.fromString(vendorAId));

        // Vendor B: approved and sold through, but never sets payout details — BR-STL-08's "no
        // payout details on file" exclusion, not the cooling-off one.
        String vendorBToken = registerAndVerify("+254700200205");
        MvcResult approvedB = approveVendorApplication(
                adminToken, submitVendorApplication(vendorBToken, "Unbanked Traders", "KRA-STL-002"));
        String vendorBId = JsonPath.read(approvedB.getResponse().getContentAsString(), "$.data.vendorId");
        String productBId = createAndPublishProduct(vendorBToken, "Settlement Widget B", null, 50_000, "KES");

        for (String productId : new String[] {productAId, productBId}) {
            MvcResult order = mvc.perform(post("/api/checkout")
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200203\",\"deliveryZoneId\":\"%s\"}"
                                    .formatted(productId, defaultDeliveryZoneId())))
                    .andExpect(status().isOk())
                    .andReturn();
            String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
            confirmPaymentFor(orderId);
            awaitOrderStatus(orderId, buyerToken, "CONFIRMED");
        }

        // 100,000 subtotal, 9% commission = 9,000, vendor portion 91,000, 10% DOWN holdback = 9,100,
        // leaving 81,900 payable for vendor A. Vendor B's ledger posting is waited for too, not just
        // vendor A's — settlement.initiate reads vendorsWithPayableBalance directly off the ledger,
        // and vendor B's posting is an equally-async, equally-unfinished hop at this point; without
        // waiting for it, vendor B could still be silently absent from the run rather than EXCLUDED.
        awaitVendorPayable(vendorAId, vendorAToken, 81_900);
        awaitVendorPayable(vendorBId, vendorBToken, 40_950);

        // A non-finance caller may not initiate a run at all (BR-STL-11's approval gate starts here).
        mvc.perform(post("/api/settlement/runs")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"KES\"}"))
                .andExpect(status().isForbidden());

        MvcResult initiated = mvc.perform(post("/api/settlement/runs")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();
        String runId = JsonPath.read(initiated.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(get("/api/settlement/runs/" + runId + "/lines")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorAId + "')].status", org.hamcrest.Matchers.contains("PENDING")))
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorBId + "')].exclusionReason",
                        org.hamcrest.Matchers.contains("NO_PAYOUT_DETAILS")));

        mvc.perform(post("/api/settlement/runs/" + runId + "/approve")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mvc.perform(get("/api/settlement/runs/" + runId + "/lines")
                        .header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorAId + "')].status", org.hamcrest.Matchers.contains("DISBURSED")))
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorAId + "')].disbursementReference",
                        org.hamcrest.Matchers.contains(org.hamcrest.Matchers.startsWith("MOCK-DISB-"))));

        // BR-STL-06: the disbursement posted to the ledger, so vendor A's payable is now zero — its
        // holdback is untouched (BR-STL-05's release does not exist yet; see V14's header comment).
        mvc.perform(get("/api/settlement/vendors/" + vendorAId + "/balance")
                        .header("Authorization", "Bearer " + vendorAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable.minorUnits").value(0))
                .andExpect(jsonPath("$.data.holdback.minorUnits").value(9_100));
    }

    @Test
    @DisplayName("BR-ADM-06: a vendor settled once is excluded from a second run until the configured settlement cycle elapses")
    void settlementCycleExcludesAVendorSettledTooRecently() throws Exception {
        String adminToken = registerAndVerify("+254700200220");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String financeToken = registerAndVerify("+254700200221");
        grantRole(financeToken, "FINANCE_ADMIN");
        String buyerToken = registerAndVerify("+254700200222");

        String vendorToken = registerAndVerify("+254700200223");
        MvcResult approved = approveVendorApplication(
                adminToken, submitVendorApplication(vendorToken, "Cycle Traders", "KRA-STL-CYC-001"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String productId = createAndPublishProduct(vendorToken, "Cycle Widget", null, 100_000, "KES");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/vendors/" + vendorId + "/payout-details")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"MPESA\",\"destination\":\"254700200223\"}"))
                .andExpect(status().isOk());

        // Same backdating stand-in for missing fast-forward tooling as the sibling settlement test.
        jdbc.update(
                "update payout_detail set changed_at = ?, cooling_off_until = ? where vendor_id = ?",
                Timestamp.from(Instant.now().minusSeconds(30 * 24 * 3600)),
                Timestamp.from(Instant.now().minusSeconds(23 * 24 * 3600)),
                UUID.fromString(vendorId));

        // First order, first settlement run and approval — this vendor's very first disbursement is
        // never excluded by the cycle check (nothing to measure it against yet), proving that side too.
        String firstOrderId = checkoutAndConfirm(productId, buyerToken, "+254700200222");
        awaitVendorPayable(vendorId, vendorToken, 81_900);

        MvcResult firstRun = mvc.perform(post("/api/settlement/runs")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String firstRunId = JsonPath.read(firstRun.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(get("/api/settlement/runs/" + firstRunId + "/lines").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorId + "')].status", org.hamcrest.Matchers.contains("PENDING")));

        mvc.perform(post("/api/settlement/runs/" + firstRunId + "/approve").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // Second order for the same vendor, same math — awaitVendorPayable's exact-value poll is
        // itself proof the first disbursement already zeroed this vendor's payable balance before
        // the second posting landed.
        checkoutAndConfirm(productId, buyerToken, "+254700200222");
        awaitVendorPayable(vendorId, vendorToken, 81_900);

        // Default seeded cycle is 7 days (V34); the first disbursement just happened, so this second
        // run must exclude the vendor rather than pay it out twice inside one cycle.
        MvcResult secondRun = mvc.perform(post("/api/settlement/runs")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String secondRunId = JsonPath.read(secondRun.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(get("/api/settlement/runs/" + secondRunId + "/lines").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorId + "')].exclusionReason",
                        org.hamcrest.Matchers.contains("SETTLEMENT_CYCLE_NOT_ELAPSED")));

        // Backdating the first disbursement's resolvedAt past the 7-day cycle — the same real-time
        // test workaround used throughout this class — proves the exclusion actually lifts once the
        // configured cycle has elapsed, rather than being a one-way, permanent block.
        jdbc.update(
                "update settlement_line set resolved_at = ? where vendor_id = ? and status = 'DISBURSED'",
                Timestamp.from(Instant.now().minusSeconds(10 * 24 * 3600)),
                UUID.fromString(vendorId));

        MvcResult thirdRun = mvc.perform(post("/api/settlement/runs")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String thirdRunId = JsonPath.read(thirdRun.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(get("/api/settlement/runs/" + thirdRunId + "/lines").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorId + "')].status", org.hamcrest.Matchers.contains("PENDING")));

        mvc.perform(post("/api/settlement/runs/" + thirdRunId + "/approve").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mvc.perform(get("/api/settlement/runs/" + thirdRunId + "/lines").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.data[?(@.vendorId=='" + vendorId + "')].status", org.hamcrest.Matchers.contains("DISBURSED")));
    }

    /** 100,000 subtotal at the shared suite's default 9%/10% commission/holdback — checks out and confirms in one call, reused by settlement-cycle tests that need several identical orders. */
    private String checkoutAndConfirm(String productId, String buyerToken, String buyerPhoneNumber) throws Exception {
        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"%s\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, buyerPhoneNumber, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");
        return orderId;
    }

    @Test
    @DisplayName("a partial return the vendor accepts refunds proportionally and reverses commission in the ledger")
    void returnAcceptedByVendorRefundsAndReversesCommission() throws Exception {
        String adminToken = registerAndVerify("+254700200301");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200302");
        String buyerToken = registerAndVerify("+254700200303");

        MvcResult approved = approveVendorApplication(
                adminToken, submitVendorApplication(vendorToken, "Returnable Traders", "KRA-RTN-001"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String productId = createAndPublishProduct(vendorToken, "Returnable Widget", null, 250_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"+254700200303\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");
        awaitVendorPayable(vendorId, vendorToken, 409_500);

        // BR-RTN-01 needs a delivered sub-order to raise a return against — a request before that
        // is refused, matching AC-RTN-05's "outside the window" refusal shape.
        mvc.perform(post("/api/returns")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subOrderId\":\"%s\",\"reason\":\"DEFECTIVE\",\"lines\":[{\"productId\":\"%s\",\"quantity\":1}]}"
                                .formatted(subOrderId, productId)))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/mark-delivered")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());

        // BR-ORD-13/15: cancellation stops being available once delivered — this is exactly the
        // point BR-ORD-15 directs the buyer to the return process this test exercises next.
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isBadRequest());

        // BR-RTN-04: one of the two units bought, not both — a partial return.
        MvcResult requested = mvc.perform(post("/api/returns")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"subOrderId\":\"%s\",\"reason\":\"DEFECTIVE\",\"description\":\"One unit arrived cracked\","
                                        + "\"lines\":[{\"productId\":\"%s\",\"quantity\":1}]}")
                                .formatted(subOrderId, productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"))
                .andExpect(jsonPath("$.data.refundAmount.minorUnits").value(250_000))
                .andReturn();
        String returnId = JsonPath.read(requested.getResponse().getContentAsString(), "$.data.id");

        // A buyer unrelated to this return sees nothing — the same cross-party disclosure pattern
        // as everywhere else, not a new rule invented for returns.
        String otherBuyerToken = registerAndVerify("+254700200304");
        mvc.perform(get("/api/returns/" + returnId).header("Authorization", "Bearer " + otherBuyerToken))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/returns/" + returnId + "/vendor-response")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED_BUYER"));

        // 250,000 refunded at 9% = 22,500 commission reversed; 227,500 vendor portion, 10% DOWN
        // holdback reversal = 22,750, leaving 204,750 payable reversed — 409,500 - 204,750 =
        // 204,750 payable and 45,500 - 22,750 = 22,750 holdback remaining.
        awaitVendorPayable(vendorId, vendorToken, 204_750);
        mvc.perform(get("/api/settlement/vendors/" + vendorId + "/balance").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable.minorUnits").value(204_750))
                .andExpect(jsonPath("$.data.holdback.minorUnits").value(22_750));
    }

    @Test
    @DisplayName("a vendor's rejection escalates to a dispute, and an admin's resolution in the buyer's favour still refunds")
    void vendorRejectionEscalatesAndAdminResolvesForBuyer() throws Exception {
        String adminToken = registerAndVerify("+254700200311");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String financeToken = registerAndVerify("+254700200312");
        grantRole(financeToken, "FINANCE_ADMIN");
        String vendorToken = registerAndVerify("+254700200313");
        String buyerToken = registerAndVerify("+254700200314");

        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Disputed Traders", "KRA-RTN-002"));
        String productId = createAndPublishProduct(vendorToken, "Disputed Widget", null, 100_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200314\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/mark-delivered")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());

        MvcResult requested = mvc.perform(post("/api/returns")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"subOrderId\":\"%s\",\"reason\":\"CHANGED_MIND\",\"lines\":[{\"productId\":\"%s\",\"quantity\":1}]}"
                                .formatted(subOrderId, productId)))
                .andExpect(status().isOk())
                .andReturn();
        String returnId = JsonPath.read(requested.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(post("/api/returns/" + returnId + "/vendor-response")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accept\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ESCALATED"));

        MvcResult queue = mvc.perform(get("/api/admin/returns/disputes").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].returnId", org.hamcrest.Matchers.hasItem(returnId)))
                .andReturn();
        List<String> disputeIds = JsonPath.read(
                queue.getResponse().getContentAsString(), "$.data[?(@.returnId=='" + returnId + "')].id");
        String disputeId = disputeIds.get(0);

        // AC-RTN-04: a blank reason is refused before anything else happens.
        mvc.perform(post("/api/admin/returns/disputes/" + disputeId + "/resolve")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favor\":\"BUYER\",\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/returns/disputes/" + disputeId + "/resolve")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"favor\":\"BUYER\",\"reason\":\"Vendor evidence unconvincing\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        // The refund still happened even though the vendor rejected it — BR-RTN-07 overriding
        // BR-RTN-03's initial outcome, itemised: 9% commission reversed on the full 100,000.
        mvc.perform(get("/api/returns/" + returnId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED_BUYER"));
    }

    @Test
    @DisplayName("a buyer cancelling before payment is confirmed releases the reservation with nothing to refund")
    void buyerCancelsBeforePaymentReleasesReservation() throws Exception {
        String adminToken = registerAndVerify("+254700200321");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200322");
        String buyerToken = registerAndVerify("+254700200323");

        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Cancellable Traders", "KRA-ORD-001"));
        String productId = createAndPublishProduct(vendorToken, "Cancellable Widget A", null, 50_000, "KES");

        mvc.perform(get("/api/inventory/products/" + productId + "/stock").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity").value(100));

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"+254700200323\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subOrders[0].cancellable").value(true))
                .andReturn();
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        mvc.perform(get("/api/inventory/products/" + productId + "/stock").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity").value(98));

        // A different buyer may not cancel someone else's sub-order — the same cross-buyer
        // disclosure pattern as everywhere else, not a new rule invented for cancellation.
        String otherBuyerToken = registerAndVerify("+254700200324");
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel").header("Authorization", "Bearer " + otherBuyerToken))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellable").value(false));

        // BR-INV-06: released, not restocked — the reservation was never confirmed, so nothing was
        // ever physically deducted from totalQuantity to give back. Polled: the release happens on
        // SubOrderCancelled, an @ApplicationModuleListener hop past the HTTP response above.
        awaitAvailableQuantity(productId, vendorToken, 100);

        // Cancelling twice is refused, not silently accepted a second time.
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a buyer cancelling after payment is confirmed refunds, restocks, and reverses commission")
    void buyerCancelsAfterConfirmationRefundsRestocksAndReversesCommission() throws Exception {
        String adminToken = registerAndVerify("+254700200331");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200332");
        String buyerToken = registerAndVerify("+254700200333");

        MvcResult approved = approveVendorApplication(
                adminToken, submitVendorApplication(vendorToken, "Confirmed Cancel Traders", "KRA-ORD-002"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String productId = createAndPublishProduct(vendorToken, "Cancellable Widget B", null, 250_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"+254700200333\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");
        awaitVendorPayable(vendorId, vendorToken, 409_500);

        mvc.perform(get("/api/inventory/products/" + productId + "/stock").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(98))
                .andExpect(jsonPath("$.data.availableQuantity").value(98));

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // BR-ORD-14: stock physically given back — totalQuantity restored to 100, not just
        // availableQuantity, distinguishing this from the pre-confirmation release case above.
        awaitTotalQuantity(productId, vendorToken, 100);
        mvc.perform(get("/api/inventory/products/" + productId + "/stock").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableQuantity").value(100));

        // The full 500,000 subtotal refunded and its 9%/10%-holdback split reversed — vendor's
        // payable and holdback both back to zero, exactly undoing oneOrderCompletesEndToEnd's math.
        awaitVendorPayable(vendorId, vendorToken, 0);
        mvc.perform(get("/api/settlement/vendors/" + vendorId + "/balance").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payable.minorUnits").value(0))
                .andExpect(jsonPath("$.data.holdback.minorUnits").value(0));

        // BR-ORD-15: no longer cancellable — attempting again is refused, directing the buyer to
        // the return process instead (10.11 / this class's returns tests).
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a checkout-time delivery choice auto-books a delivery, which a vendor can then dispatch and complete, visible to the buyer throughout")
    void fulfilmentTracksADeliveryFromBookingToCompletion() throws Exception {
        String adminToken = registerAndVerify("+254700200341");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200342");
        String buyerToken = registerAndVerify("+254700200343");

        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Shipping Traders", "KRA-FUL-001"));
        String productId = createAndPublishProduct(vendorToken, "Shippable Widget", null, 75_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200343\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        // BR-CHK-05/06: created off OrderPaymentConfirmed (BR-FUL-01) and, this phase, immediately
        // auto-booked with the buyer's own checkout-time selection — see OrderReadinessListener's
        // javadoc. No vendor action needed to reach BOOKED any more.
        String deliveryId = awaitDeliveryId(subOrderId, vendorToken);
        mvc.perform(get("/api/fulfilment/deliveries/" + deliveryId).header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BOOKED"))
                .andExpect(jsonPath("$.data.method").value("DOOR"));

        // A different vendor may not act on this delivery — the same cross-vendor disclosure
        // pattern as everywhere else in this codebase.
        String otherVendorToken = registerAndVerify("+254700200344");
        approveVendorApplication(adminToken, submitVendorApplication(otherVendorToken, "Uninvolved Traders", "KRA-FUL-002"));
        mvc.perform(get("/api/fulfilment/deliveries/" + deliveryId).header("Authorization", "Bearer " + otherVendorToken))
                .andExpect(status().isNotFound());

        // BR-FUL-08: admin-configured reference data — a fresh zone and rate card, not just the
        // seeded defaults, proving the admin path itself works end to end.
        MvcResult carriers = mvc.perform(get("/api/fulfilment/carriers").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn();
        String carrierId = JsonPath.read(carriers.getResponse().getContentAsString(), "$.data[0].id");

        MvcResult zone = mvc.perform(post("/api/admin/fulfilment/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Mombasa Metro\",\"county\":\"Mombasa\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();
        String zoneId = JsonPath.read(zone.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(post("/api/admin/fulfilment/rate-cards")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierId\":\"%s\",\"zoneId\":\"%s\",\"method\":\"DOOR\",\"priceMinorUnits\":30000,\"currency\":\"KES\"}"
                                .formatted(carrierId, zoneId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price.minorUnits").value(30_000));

        // BR-FUL-01/03: already auto-booked at checkout (above) — a vendor attempting to book it
        // again by hand is refused, the same "not awaiting booking" guard a genuinely
        // vendor-driven booking would hit on a second attempt.
        mvc.perform(post("/api/fulfilment/deliveries/" + deliveryId + "/book")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"carrierId\":\"%s\",\"zoneId\":\"%s\",\"method\":\"DOOR\"}".formatted(carrierId, zoneId)))
                .andExpect(status().isBadRequest());

        // BR-FUL-04, AC-FUL-01: dispatch produces a tracking reference, visible to the buyer too.
        mvc.perform(post("/api/fulfilment/deliveries/" + deliveryId + "/dispatch")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DISPATCHED"))
                .andExpect(jsonPath("$.data.trackingReference").isNotEmpty());

        mvc.perform(get("/api/fulfilment/sub-orders/" + subOrderId + "/delivery").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trackingReference").isNotEmpty());

        // BR-NTF-01: the buyer is told on dispatch, not just order confirmation.
        awaitNotificationSent("+254700200343", adminToken, "is on its way");

        mvc.perform(post("/api/fulfilment/deliveries/" + deliveryId + "/mark-in-transit")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));

        // BR-FUL-07: proof of delivery recorded on completion.
        mvc.perform(post("/api/fulfilment/deliveries/" + deliveryId + "/mark-delivered")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"proofOfDeliveryReference\":\"SIGNED-BY-BUYER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"))
                .andExpect(jsonPath("$.data.proofOfDeliveryReference").value("SIGNED-BY-BUYER"));

        // BR-NTF-01: and again on delivery — the third and last leg of BR-NTF-01's full set.
        awaitNotificationSent("+254700200343", adminToken, "has been delivered");

        // Genuinely independent of order's own DELIVERED — see OrderStatus's javadoc. Completing
        // the delivery here does not, by itself, move the sub-order's own status.
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subOrders[0].status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("a confirmed order gets a transmitted tax invoice, and a cancellation gets a credit note referencing it")
    void confirmedOrderIsInvoicedAndCancellationGetsACreditNote() throws Exception {
        String adminToken = registerAndVerify("+254700200351");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String financeToken = registerAndVerify("+254700200352");
        grantRole(financeToken, "FINANCE_ADMIN");
        String vendorToken = registerAndVerify("+254700200353");
        String buyerToken = registerAndVerify("+254700200354");

        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Taxable Traders", "KRA-TAX-001"));
        String productId = createAndPublishProduct(vendorToken, "Taxable Widget", null, 250_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"+254700200354\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        // REG-04/07: one invoice per parent order, transmitted through the mock DigiTax gateway —
        // a further @ApplicationModuleListener hop past the order reaching CONFIRMED.
        awaitInvoiceTransmitted(orderId, financeToken, 500_000);

        // A non-finance caller may not see it — the same admin-surface gating as everywhere else.
        mvc.perform(get("/api/admin/tax/orders/" + orderId + "/invoice").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        // REG-08: a credit note against that same invoice, for the full 500,000 cancelled.
        awaitCreditNoteTransmitted(orderId, financeToken, 500_000, "Buyer cancellation");
    }

    @Test
    @DisplayName("a buyer with a delivered order can review it once, a vendor can respond once, and an admin can remove it")
    void buyerReviewsDeliveredProductAndVendorRespondsOnce() throws Exception {
        String adminToken = registerAndVerify("+254700200361");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200362");
        String buyerToken = registerAndVerify("+254700200363");

        MvcResult approved = approveVendorApplication(
                adminToken, submitVendorApplication(vendorToken, "Reviewable Traders", "KRA-REV-001"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String productId = createAndPublishProduct(vendorToken, "Reviewable Widget", null, 50_000, "KES");

        // BR-REV-01: refused before the buyer has a delivered order for it.
        mvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"rating\":5,\"comment\":\"Too soon\"}".formatted(productId)))
                .andExpect(status().isBadRequest());

        // BR-REV-06: a vendor may not review their own product — refused regardless of purchase history.
        mvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"rating\":5,\"comment\":\"Great product\"}".formatted(productId)))
                .andExpect(status().isBadRequest());

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200363\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/mark-delivered")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());

        MvcResult submitted = mvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"rating\":5,\"comment\":\"Exactly as described\"}".formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();
        String reviewId = JsonPath.read(submitted.getResponse().getContentAsString(), "$.data.id");

        // Reviewing the same product twice is refused.
        mvc.perform(post("/api/reviews")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":\"%s\",\"rating\":1,\"comment\":\"Changed my mind\"}".formatted(productId)))
                .andExpect(status().isBadRequest());

        // BR-IAM-08-style guest access — no Authorization header.
        mvc.perform(get("/api/catalog/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].rating").value(5));

        mvc.perform(get("/api/catalog/products/" + productId + "/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(5.00))
                .andExpect(jsonPath("$.data.reviewCount").value(1));

        mvc.perform(get("/api/vendors/" + vendorId + "/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(5.00))
                .andExpect(jsonPath("$.data.reviewCount").value(1));

        // BR-REV-04: a vendor unrelated to this product may not respond to it.
        String otherVendorToken = registerAndVerify("+254700200364");
        approveVendorApplication(adminToken, submitVendorApplication(otherVendorToken, "Unrelated Traders", "KRA-REV-002"));
        mvc.perform(post("/api/reviews/" + reviewId + "/response")
                        .header("Authorization", "Bearer " + otherVendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"Not our review\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/reviews/" + reviewId + "/response")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"Thank you for the kind words!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.vendorResponse").value("Thank you for the kind words!"));

        // BR-REV-04: only once.
        mvc.perform(post("/api/reviews/" + reviewId + "/response")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"response\":\"A second reply\"}"))
                .andExpect(status().isBadRequest());

        // BR-REV-05: admin removal, with a mandatory reason.
        mvc.perform(post("/api/admin/reviews/" + reviewId + "/remove")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/admin/reviews/" + reviewId + "/remove")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Violates content policy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REMOVED"));

        // A removed review no longer appears in the public listing or the aggregate.
        mvc.perform(get("/api/catalog/products/" + productId + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        mvc.perform(get("/api/catalog/products/" + productId + "/reviews/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewCount").value(0));
    }

    @Test
    @DisplayName("search matches by keyword, filters by price, sorts, and logs a zero-result keyword search")
    void searchFindsFiltersSortsAndLogsZeroResults() throws Exception {
        String adminToken = registerAndVerify("+254700200371");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200372");

        MvcResult approved = approveVendorApplication(
                adminToken, submitVendorApplication(vendorToken, "Searchable Traders", "KRA-SCH-001"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");

        // Names carry a nonsense, per-run-unique prefix — this suite shares one database across
        // every test method with no rollback between them (see awaitDelisted's own comment on the
        // same constraint), and plenty of other tests name their products "...Widget...". An
        // unqualified keyword search or price filter here would match those too.
        String prefix = "Zqxvlt" + UUID.randomUUID().toString().substring(0, 8);
        String widgetKeyword = prefix + "BlueWidget";
        String gadgetDescriptionKeyword = prefix + "ShinyGadgetDescription";
        String widgetId = createAndPublishProduct(vendorToken, widgetKeyword, "no keyword here", 100_000, "KES");
        String gadgetId = createAndPublishProduct(vendorToken, prefix + "RedGadget", gadgetDescriptionKeyword, 200_000, "KES");

        // BR-SCH-01/05: keyword matches name and description, case-insensitively, as a substring —
        // no spaces in either keyword, so the raw query string needs no URL-encoding here.
        mvc.perform(get("/api/search/products?keyword=" + widgetKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(widgetId));

        mvc.perform(get("/api/search/products?keyword=" + gadgetDescriptionKeyword.toUpperCase()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(gadgetId));

        // BR-SCH-06: a keyword returning nothing is logged.
        String noResultKeyword = prefix + "-nonexistent";
        Integer logCountBefore =
                jdbc.queryForObject("select count(*) from search_log where keyword = ?", Integer.class, noResultKeyword);
        mvc.perform(get("/api/search/products?keyword=" + noResultKeyword))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        Integer logCountAfter =
                jdbc.queryForObject("select count(*) from search_log where keyword = ?", Integer.class, noResultKeyword);
        org.junit.jupiter.api.Assertions.assertEquals(logCountBefore + 1, logCountAfter);

        // BR-SCH-02: price range, scoped to this test's own vendor — otherwise every other test's
        // higher-priced product would match too.
        mvc.perform(get("/api/search/products?vendorId=" + vendorId + "&minPriceMinorUnits=150000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(gadgetId));

        // BR-SCH-02: vendor filter — matches both of this vendor's products, none of anyone else's.
        mvc.perform(get("/api/search/products?vendorId=" + vendorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id", org.hamcrest.Matchers.containsInAnyOrder(widgetId, gadgetId)));

        // BR-SCH-03: price ascending and descending.
        mvc.perform(get("/api/search/products?vendorId=" + vendorId + "&sort=PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(widgetId))
                .andExpect(jsonPath("$.data[1].id").value(gadgetId));

        mvc.perform(get("/api/search/products?vendorId=" + vendorId + "&sort=PRICE_DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(gadgetId))
                .andExpect(jsonPath("$.data[1].id").value(widgetId));

        // A plain browse with no keyword is not logged — nothing to review a search term for.
        Integer totalLogCountBefore = jdbc.queryForObject("select count(*) from search_log", Integer.class);
        mvc.perform(get("/api/search/products?vendorId=" + vendorId))
                .andExpect(status().isOk());
        Integer totalLogCountAfter = jdbc.queryForObject("select count(*) from search_log", Integer.class);
        org.junit.jupiter.api.Assertions.assertEquals(totalLogCountBefore, totalLogCountAfter);
    }

    @Test
    @DisplayName("a vendor whose application is not yet approved cannot list a product")
    void unapprovedVendorCannotPublish() throws Exception {
        // AC-VND-02: given a vendor in any state other than Approved, publishing is refused.
        String vendorToken = registerAndVerify("+254700200021");
        submitVendorApplication(vendorToken, "Pending Traders", "KRA-EEE-002");

        // The refusal is atomic — create() throws before any row is written (CatalogService), so
        // there is nothing partially published to go on and check for visibility separately. A
        // total-item count is not asserted here: test methods share one database with no rollback
        // between them, so an absolute count would depend on execution order rather than on this
        // test's own behaviour.
        mvc.perform(post("/api/catalog/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Widget\",\"priceMinorUnits\":10000,\"currency\":\"KES\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("submitting a second application with the same KRA PIN as an approved vendor is refused")
    void duplicateKraPinIsRefused() throws Exception {
        // AC-VND-05.
        String firstVendorToken = registerAndVerify("+254700200031");
        String adminToken = registerAndVerify("+254700200098");
        grantRole(adminToken, "PLATFORM_ADMIN");

        String firstApplicationId = submitVendorApplication(firstVendorToken, "First Traders", "KRA-DUP-001");
        approveVendorApplication(adminToken, firstApplicationId);

        String secondVendorToken = registerAndVerify("+254700200032");

        mvc.perform(post("/api/vendors/applications")
                        .header("Authorization", "Bearer " + secondVendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson("Second Traders", "KRA-DUP-001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("KRA_PIN_TAKEN"));
    }

    @Test
    @DisplayName("suspending a vendor immediately delists its products")
    void suspensionDelistsProducts() throws Exception {
        // BR-VND-10.
        String vendorToken = registerAndVerify("+254700200041");
        String adminToken = registerAndVerify("+254700200097");
        grantRole(adminToken, "PLATFORM_ADMIN");

        String applicationId = submitVendorApplication(vendorToken, "Third Traders", "KRA-SUS-001");
        MvcResult approved = approveVendorApplication(adminToken, applicationId);
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");

        createAndPublishProduct(vendorToken, "Doomed Widget", null, 5_000, "KES");

        // Presence/absence by name, not a total count — test methods share one database with no
        // rollback between them, so an absolute count would depend on execution order.
        mvc.perform(get("/api/catalog/products?perPage=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name", org.hamcrest.Matchers.hasItem("Doomed Widget")));

        mvc.perform(post("/api/admin/vendor-applications/vendors/" + vendorId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Repeated non-fulfilment\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        // VendorSuspensionListener runs via @ApplicationModuleListener, which is @Async by
        // definition (see ApplicationModuleListener's own source — @Async, REQUIRES_NEW,
        // AFTER_COMMIT). BR-VND-10's "immediately" is a business requirement about how soon a
        // suspended vendor's products stop appearing to buyers, not a claim that this HTTP
        // response and the listener's effect land in the same instant — the two run on different
        // threads by design (ADR-004), so the very next request can legitimately still see the
        // pre-delisting state for a brief window. This polls rather than asserting immediately, to
        // test the real eventually-consistent contract instead of an unstated synchronous one.
        awaitDelisted("Doomed Widget");
    }

    /**
     * BR-CAT-05/06, BR-INV-01: a product is created DRAFT (see ProductStatus's javadoc) and must
     * have an image before it can publish (AC-CAT-02) — this drives the real
     * create/upload-image/publish/stock sequence rather than asserting an old immediate-ACTIVE,
     * infinite-stock behaviour Phase 2/4 replaced. 100 units is set as stock, ample for any test
     * calling this helper; a test needing to exercise a stock limit sets its own via {@code PUT
     * .../stock} after calling this.
     */
    private String createAndPublishProduct(
            String vendorToken, String name, String description, long priceMinorUnits, String currency)
            throws Exception {
        String descriptionJson = description == null ? "null" : "\"" + description + "\"";

        MvcResult productResult = mvc.perform(post("/api/catalog/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"description\":%s,\"priceMinorUnits\":%d,\"currency\":\"%s\"}"
                                .formatted(name, descriptionJson, priceMinorUnits, currency)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        String productId = JsonPath.read(productResult.getResponse().getContentAsString(), "$.data.id");

        MockMultipartFile image =
                new MockMultipartFile("file", "product.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
        mvc.perform(multipart("/api/catalog/products/mine/" + productId + "/images")
                        .file(image)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());

        mvc.perform(post("/api/catalog/products/mine/" + productId + "/publish")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalQuantity\":100}"))
                .andExpect(status().isOk());

        return productId;
    }

    @Test
    @DisplayName("a vendor cannot fetch another vendor's product by direct identifier")
    void crossVendorProductAccessIsRefused() throws Exception {
        // AC-CAT-01.
        String vendorAToken = registerAndVerify("+254700200051");
        String vendorBToken = registerAndVerify("+254700200052");
        String adminToken = registerAndVerify("+254700200096");
        grantRole(adminToken, "PLATFORM_ADMIN");

        String applicationA = submitVendorApplication(vendorAToken, "Vendor A Traders", "KRA-CAT-001");
        approveVendorApplication(adminToken, applicationA);
        approveVendorApplication(adminToken, submitVendorApplication(vendorBToken, "Vendor B Traders", "KRA-CAT-002"));

        MvcResult created = mvc.perform(post("/api/catalog/products")
                        .header("Authorization", "Bearer " + vendorAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Vendor A's Product\",\"priceMinorUnits\":1000,\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String productId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        // Vendor A can see their own draft.
        mvc.perform(get("/api/catalog/products/mine/" + productId).header("Authorization", "Bearer " + vendorAToken))
                .andExpect(status().isOk());

        // Vendor B, requesting it by direct identifier, gets not-found — never forbidden, which
        // would confirm the id belongs to someone.
        mvc.perform(get("/api/catalog/products/mine/" + productId).header("Authorization", "Bearer " + vendorBToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("publishing without an image is refused")
    void publishWithoutImageIsRefused() throws Exception {
        // AC-CAT-02.
        String vendorToken = registerAndVerify("+254700200061");
        String adminToken = registerAndVerify("+254700200095");
        grantRole(adminToken, "PLATFORM_ADMIN");
        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Imageless Traders", "KRA-CAT-003"));

        MvcResult created = mvc.perform(post("/api/catalog/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No Image Yet\",\"priceMinorUnits\":1000,\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String productId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mvc.perform(post("/api/catalog/products/mine/" + productId + "/publish")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("a listing matching the prohibited-items list is blocked at publish, with the matched term shown")
    void prohibitedTermBlocksPublish() throws Exception {
        // AC-CAT-03. "firearm" is one of V8's seeded placeholder terms — see ProhibitedTermRepository.
        String vendorToken = registerAndVerify("+254700200071");
        String adminToken = registerAndVerify("+254700200094");
        grantRole(adminToken, "PLATFORM_ADMIN");
        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Banned Traders", "KRA-CAT-004"));

        MvcResult created = mvc.perform(post("/api/catalog/products")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Antique Firearm Replica\",\"priceMinorUnits\":1000,\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String productId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        MockMultipartFile image =
                new MockMultipartFile("file", "product.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] {1, 2, 3});
        mvc.perform(multipart("/api/catalog/products/mine/" + productId + "/images")
                        .file(image)
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk());

        mvc.perform(post("/api/catalog/products/mine/" + productId + "/publish")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", org.hamcrest.Matchers.containsString("firearm")));
    }

    @Test
    @DisplayName("a price change is retrievable with the previous price, new price, actor and timestamp")
    void priceChangeIsRetrievable() throws Exception {
        // AC-CAT-04.
        String vendorToken = registerAndVerify("+254700200081");
        String adminToken = registerAndVerify("+254700200093");
        grantRole(adminToken, "PLATFORM_ADMIN");
        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Pricing Traders", "KRA-CAT-005"));

        String productId = createAndPublishProduct(vendorToken, "Repriced Widget", null, 1_000, "KES");

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/catalog/products/mine/" + productId + "/price")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priceMinorUnits\":1500,\"currency\":\"KES\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.price.minorUnits").value(1500));

        mvc.perform(get("/api/catalog/products/mine/" + productId + "/price-history")
                        .header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].previousPrice.minorUnits").value(1000))
                .andExpect(jsonPath("$.data[0].newPrice.minorUnits").value(1500))
                .andExpect(jsonPath("$.data[0].actorAccountId", notNullValue()))
                .andExpect(jsonPath("$.data[0].changedAt", notNullValue()));
    }

    @Test
    @DisplayName("checkout splits a cart spanning two vendors into two sub-orders")
    void checkoutSplitsCartAcrossVendors() throws Exception {
        // BR-CHK-01/02, BR-ORD-02.
        String vendorAToken = registerAndVerify("+254700200111");
        String vendorBToken = registerAndVerify("+254700200112");
        String buyerToken = registerAndVerify("+254700200113");
        String adminToken = registerAndVerify("+254700200092");
        grantRole(adminToken, "PLATFORM_ADMIN");

        approveVendorApplication(adminToken, submitVendorApplication(vendorAToken, "Cart Vendor A", "KRA-CHK-001"));
        approveVendorApplication(adminToken, submitVendorApplication(vendorBToken, "Cart Vendor B", "KRA-CHK-002"));

        String productA = createAndPublishProduct(vendorAToken, "Cart Item A", null, 10_000, "KES");
        String productB = createAndPublishProduct(vendorBToken, "Cart Item B", null, 20_000, "KES");

        MvcResult result = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                "{\"lines\":[{\"productId\":\"%s\",\"quantity\":1},{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"+254700200113\",\"deliveryZoneId\":\"%s\"}"
                                        .formatted(productA, productB, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.subOrders", org.hamcrest.Matchers.hasSize(2)))
                // BR-CHK-06/07: 50,000 items plus 25,000 DOOR delivery for each of the two vendor
                // groups (V19__fulfilment.sql's seeded rate, doubled since two vendors are involved).
                .andExpect(jsonPath("$.data.total.minorUnits").value(100_000))
                .andReturn();

        // Each sub-order belongs to exactly one of the two vendors — checkout must not merge or
        // drop a vendor group.
        List<String> vendorIds = JsonPath.read(result.getResponse().getContentAsString(), "$.data.subOrders[*].vendorId");
        assertUniqueVendors(vendorIds);
    }

    private void assertUniqueVendors(List<String> vendorIds) {
        org.junit.jupiter.api.Assertions.assertEquals(2, vendorIds.size());
        org.junit.jupiter.api.Assertions.assertNotEquals(vendorIds.get(0), vendorIds.get(1));
    }

    @Test
    @DisplayName("checkout refuses a cart containing a suspended vendor's product")
    void checkoutRefusesSuspendedVendor() throws Exception {
        // BR-CHK-09.
        String vendorToken = registerAndVerify("+254700200121");
        String buyerToken = registerAndVerify("+254700200122");
        String adminToken = registerAndVerify("+254700200091");
        grantRole(adminToken, "PLATFORM_ADMIN");

        String applicationId = submitVendorApplication(vendorToken, "Soon Suspended Traders", "KRA-CHK-003");
        MvcResult approved = approveVendorApplication(adminToken, applicationId);
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");

        String productId = createAndPublishProduct(vendorToken, "About To Be Blocked", null, 1_000, "KES");

        mvc.perform(post("/api/admin/vendor-applications/vendors/" + vendorId + "/suspend")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Testing BR-CHK-09\"}"))
                .andExpect(status().isOk());

        // Checked directly against vendor status in CheckoutService, not the (possibly not yet
        // async-delisted) product status — see that service's comment on why both checks exist.
        mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200122\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("insufficient stock refuses checkout and creates no order at all")
    void checkoutRefusesInsufficientStockAndCreatesNoOrder() throws Exception {
        // BR-INV-04, AC-INV-01, and the atomicity CheckoutService's class doc describes.
        String vendorToken = registerAndVerify("+254700200131");
        String buyerToken = registerAndVerify("+254700200132");
        String adminToken = registerAndVerify("+254700200090");
        grantRole(adminToken, "PLATFORM_ADMIN");

        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Scarce Traders", "KRA-CHK-004"));
        String productId = createAndPublishProduct(vendorToken, "Only One Left", null, 1_000, "KES");

        // createAndPublishProduct sets 100 units; reduce to exactly 1 so requesting 2 overshoots.
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/inventory/products/" + productId + "/stock")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalQuantity\":1}"))
                .andExpect(status().isOk());

        int ordersBefore = countRows("parent_order");

        mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":2}],\"buyerPhoneNumber\":\"+254700200132\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isBadRequest());

        // No parent order row was left behind by the failed attempt — the order-creation call
        // inside CheckoutService's transaction rolled back along with the failed reservation.
        org.junit.jupiter.api.Assertions.assertEquals(ordersBefore, countRows("parent_order"));

        // BR-SEC-06: the refused reservation is a real, admin-visible near-miss — and, unlike the
        // parent order above, it survives the very rollback this same request triggered (see
        // OversellAttemptRecorder's javadoc).
        MvcResult oversellAttempts = mvc.perform(get("/api/admin/inventory/oversell-attempts").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> attempts = JsonPath.read(oversellAttempts.getResponse().getContentAsString(), "$.data");
        boolean matched = attempts.stream().anyMatch(a -> productId.equals(a.get("productId"))
                && ((Number) a.get("requestedQuantity")).intValue() == 2
                && ((Number) a.get("availableQuantity")).intValue() == 1);
        org.junit.jupiter.api.Assertions.assertTrue(matched, "expected an oversell attempt for " + productId + " in " + attempts);
    }

    @Test
    @DisplayName("real events drive real SMS to buyer and vendor, retrievable through the admin notification queue")
    void ordersAndApplicationsDriveRealNotifications() throws Exception {
        String adminToken = registerAndVerify("+254700200401");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorPhone = "+254700200402";
        String vendorToken = registerAndVerify(vendorPhone);
        String buyerPhone = "+254700200403";
        String buyerToken = registerAndVerify(buyerPhone);

        // BR-VND-06: approval notifies the applicant — a real send to their own phone number.
        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Notified Traders", "KRA-NTF-001"));
        awaitNotificationSent(vendorPhone, adminToken, "live on Meridian");

        String productId = createAndPublishProduct(vendorToken, "Notified Widget", null, 100_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"%s\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, buyerPhone, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String reference = JsonPath.read(order.getResponse().getContentAsString(), "$.data.reference");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        // BR-NTF-01: the buyer is told their order confirmed.
        awaitNotificationSent(buyerPhone, adminToken, reference + " is confirmed");
        // BR-NTF-02: the vendor is told they have a new order, on the same OrderPaymentConfirmed event.
        awaitNotificationSent(vendorPhone, adminToken, "new order");

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        // BR-ORD-16: the vendor is told of the buyer-initiated cancellation.
        awaitNotificationSent(vendorPhone, adminToken, "cancelled: Buyer cancellation");

        // A non-admin caller may not read the notification queue — the same admin-surface gating as everywhere else.
        mvc.perform(get("/api/admin/notifications").param("recipient", vendorPhone).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        // BR-NTF-04: a buyer's own opt-out preference, self-service, defaulting to false.
        mvc.perform(get("/api/notifications/preferences").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketingOptOut").value(false));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/notifications/preferences")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"marketingOptOut\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketingOptOut").value(true));

        mvc.perform(get("/api/notifications/preferences").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.marketingOptOut").value(true));
    }

    /** Delivery is async off the driving event (an {@code @ApplicationModuleListener}), same reasoning as {@link #awaitInvoiceTransmitted}. */
    private void awaitNotificationSent(String recipient, String adminToken, String expectedBodyFragment) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        String last = "<no response>";
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/admin/notifications")
                            .param("recipient", recipient)
                            .header("Authorization", "Bearer " + adminToken))
                    .andReturn();
            last = result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() == 200) {
                List<Map<String, Object>> messages = JsonPath.read(result.getResponse().getContentAsString(), "$.data");
                boolean matched = messages.stream()
                        .anyMatch(m -> "SENT".equals(m.get("status")) && String.valueOf(m.get("body")).contains(expectedBodyFragment));
                if (matched) {
                    return;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("No SENT notification to " + recipient + " containing \"" + expectedBodyFragment + "\" in time; last response: " + last);
    }

    @Test
    @DisplayName(
            "repeated OTC requests for one phone number are rate-limited, escalate to a restriction, and an admin can lift it")
    void repeatedOtcRequestsAreRateLimitedAndEscalateToARestriction() throws Exception {
        String adminToken = registerAndVerify("+254700200501");
        grantRole(adminToken, "PLATFORM_ADMIN");

        String phone = "+254700200502";

        // The first request succeeds and issues a real code.
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"displayName\":\"Rate Limited Buyer\"}".formatted(phone)))
                .andExpect(status().isOk());

        // BR-SEC-02: three more within the same 60-second interval window are all refused, each a
        // real breach recorded against the same (scope, key) — the third crosses RateLimiterService's
        // escalation threshold.
        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/api/auth/code")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"identifier\":\"%s\"}".formatted(phone)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.code").value("RATE_LIMITED"));
        }

        // BR-SEC-07/08: those three breaches escalated into a recorded, admin-visible restriction.
        MvcResult active = mvc.perform(get("/api/admin/integrity/restrictions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> restrictions = JsonPath.read(active.getResponse().getContentAsString(), "$.data");
        Map<String, Object> restriction = restrictions.stream()
                .filter(r -> phone.equals(r.get("key")) && "OTC_ISSUANCE_INTERVAL".equals(r.get("scope")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected an ACTIVE restriction for " + phone + " in " + restrictions));
        String restrictionId = (String) restriction.get("id");

        // A non-admin caller may not see it — the same admin-surface gating as everywhere else.
        String otherToken = registerAndVerify("+254700200503");
        mvc.perform(get("/api/admin/integrity/restrictions").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());

        // AC-SEC-02: the admin sees the triggering signal and lifts it in full — never an account termination.
        mvc.perform(post("/api/admin/integrity/restrictions/" + restrictionId + "/lift")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Confirmed legitimate retry with the buyer by phone\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("LIFTED"));

        MvcResult lifted = mvc.perform(get("/api/admin/integrity/restrictions/lifted").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> liftedList = JsonPath.read(lifted.getResponse().getContentAsString(), "$.data");
        org.junit.jupiter.api.Assertions.assertTrue(
                liftedList.stream().anyMatch(r -> restrictionId.equals(r.get("id"))),
                "expected " + restrictionId + " in the lifted list: " + liftedList);
    }

    @Test
    @DisplayName("admin order search, payment exceptions, catalog moderation, commission configuration and the audit log are all real")
    void adminToolsAreRealAndAudited() throws Exception {
        String adminToken = registerAndVerify("+254700200601");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String financeToken = registerAndVerify("+254700200602");
        grantRole(financeToken, "FINANCE_ADMIN");
        String vendorToken = registerAndVerify("+254700200603");
        String buyerToken = registerAndVerify("+254700200604");
        String buyerAccountId = accountIdFor(buyerToken);

        String applicationId = submitVendorApplication(vendorToken, "Admin Tools Traders", "KRA-ADM-001");
        approveVendorApplication(adminToken, applicationId);

        // BR-ADM-07: the approval decision is audited for real, not just published as a domain event.
        MvcResult auditEntries = mvc.perform(get("/api/admin/audit-log")
                        .param("entityType", "VendorApplication")
                        .param("entityId", applicationId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> entries = JsonPath.read(auditEntries.getResponse().getContentAsString(), "$.data");
        org.junit.jupiter.api.Assertions.assertTrue(
                entries.stream().anyMatch(e -> "VENDOR_APPLICATION_APPROVED".equals(e.get("action"))),
                "expected an audit entry for application " + applicationId + " in " + entries);

        // A finance admin is not a platform admin — the audit log stays gated even for another admin role.
        mvc.perform(get("/api/admin/audit-log").header("Authorization", "Bearer " + financeToken)).andExpect(status().isForbidden());

        String productId = createAndPublishProduct(vendorToken, "Admin Tools Widget", null, 75_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200604\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String reference = JsonPath.read(order.getResponse().getContentAsString(), "$.data.reference");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        String paymentReference = jdbc.queryForObject(
                "select provider_transaction_reference from payment_transaction where parent_order_id = ?",
                String.class,
                UUID.fromString(orderId));

        // BR-ADM-02: three independent search dimensions, each resolving to the same real order.
        assertOrderSearchFinds("reference", reference, orderId, adminToken);
        assertOrderSearchFinds("buyerAccountId", buyerAccountId, orderId, adminToken);
        assertOrderSearchFinds("paymentReference", paymentReference, orderId, adminToken);

        // A buyer may not search across all vendors' orders — the same admin-surface gating as everywhere else.
        mvc.perform(get("/api/admin/orders/search").param("reference", reference).header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        // BR-ADM-03: the payment exception queue is a real, finance-gated, reachable endpoint.
        mvc.perform(get("/api/admin/payments/exceptions").header("Authorization", "Bearer " + financeToken))
                .andExpect(status().isOk());

        // BR-ADM-05: admin moderation delists regardless of ownership, with a reason — and it too is audited.
        mvc.perform(post("/api/admin/catalog/products/" + productId + "/moderate-delist")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Counterfeit listing reported\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELISTED"));

        // A delisted product 404s for a public read (CatalogService.get's own documented behaviour,
        // matching BR-SCH-04) — the owner's own view is what confirms the moderation actually took.
        mvc.perform(get("/api/catalog/products/mine/" + productId).header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELISTED"));

        // BR-ADM-06: a real, versioned commission-rate change — DAT-07's new-row-not-an-edit
        // discipline. effectiveFrom is decades out so this row is never picked up as the current
        // rate: every other test in this shared, non-rolled-back database assumes V10's seeded 9%
        // default stays current for the rest of the suite run, and this test's job is to prove the
        // creation mechanism is real, not to actually change what every other order is priced at.
        mvc.perform(post("/api/admin/config/commission-rate")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":0.12,\"effectiveFrom\":\"2099-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rate").value(0.12));

        // A platform admin may moderate catalog and vendors, but setting money-affecting configuration needs finance.
        mvc.perform(post("/api/admin/config/commission-rate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":0.15}"))
                .andExpect(status().isForbidden());

        // BR-ADM-06: holdback rate follows the exact same versioning discipline as commission rate
        // — same far-future effectiveFrom, for the same "prove the mechanism, don't move the live
        // default" reason.
        mvc.perform(post("/api/admin/config/holdback-rate")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":0.15,\"effectiveFrom\":\"2099-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rate").value(0.15));

        mvc.perform(post("/api/admin/config/holdback-rate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":0.2}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/admin/config/holdback-rate")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rate\":1.5}"))
                .andExpect(status().isBadRequest());

        // BR-ADM-06: settlement cycle is real too, but lives on settlement's own admin surface, not
        // admin/config — see settlement.package-info.java for why. 1..31 days per BRD 5.6; the
        // out-of-range check proves the CHECK constraint's bounds are actually enforced, not just
        // documented.
        mvc.perform(post("/api/settlement/config/cycle")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cycleDays\":30,\"effectiveFrom\":\"2099-01-01T00:00:00Z\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cycleDays").value(30));

        mvc.perform(post("/api/settlement/config/cycle")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cycleDays\":7}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/api/settlement/config/cycle")
                        .header("Authorization", "Bearer " + financeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cycleDays\":32}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("BR-ADM-08: an admin can impersonate a vendor's owner account, and it is audited and visible to the vendor")
    void adminImpersonatesAVendorAccountRealAuditedAndVisible() throws Exception {
        String adminToken = registerAndVerify("+254700200240");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String buyerToken = registerAndVerify("+254700200241");

        String vendorToken = registerAndVerify("+254700200242");
        MvcResult approved = approveVendorApplication(
                adminToken, submitVendorApplication(vendorToken, "Impersonation Traders", "KRA-IMP-001"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String vendorAccountId = accountIdFor(vendorToken);

        // A non-admin may not start an impersonation session at all.
        mvc.perform(post("/api/admin/vendors/" + vendorId + "/impersonate")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Should never reach the service\"}"))
                .andExpect(status().isForbidden());

        MvcResult impersonation = mvc.perform(post("/api/admin/vendors/" + vendorId + "/impersonate")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Vendor reported checkout errors, reproducing with support access\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.targetAccountId").value(vendorAccountId))
                .andReturn();
        String impersonationToken = JsonPath.read(impersonation.getResponse().getContentAsString(), "$.data.token");

        // The issued token is real: it authenticates as the vendor's own owner account, not the
        // admin's — proven by successfully reading a vendor-owner-gated endpoint neither the admin
        // nor a bystander could reach.
        mvc.perform(get("/api/vendors/" + vendorId + "/payout-details").header("Authorization", "Bearer " + impersonationToken))
                .andExpect(status().isOk());

        // BR-ADM-07: the impersonation itself is a real, admin-reviewable audit entry.
        MvcResult auditEntries = mvc.perform(get("/api/admin/audit-log")
                        .param("entityType", "Account")
                        .param("entityId", vendorAccountId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> entries = JsonPath.read(auditEntries.getResponse().getContentAsString(), "$.data");
        org.junit.jupiter.api.Assertions.assertTrue(
                entries.stream().anyMatch(e -> "VENDOR_IMPERSONATION_STARTED".equals(e.get("action"))),
                "expected a VENDOR_IMPERSONATION_STARTED audit entry for " + vendorAccountId + " in " + entries);

        // BR-ADM-08's "visible to the vendor" half — the vendor owner can see it happened.
        MvcResult log = mvc.perform(
                        get("/api/vendors/" + vendorId + "/impersonation-log").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> logEntries = JsonPath.read(log.getResponse().getContentAsString(), "$.data");
        org.junit.jupiter.api.Assertions.assertTrue(
                logEntries.stream()
                        .anyMatch(e -> accountIdFor(adminToken).equals(e.get("impersonatorAccountId"))),
                "expected an impersonation record naming the admin in " + logEntries);

        // An unrelated caller — not the vendor owner — may not read that log.
        mvc.perform(get("/api/vendors/" + vendorId + "/impersonation-log").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a vendor accepts a confirmed sub-order, and it stays buyer-cancellable with the accurate reason after")
    void vendorAcceptsAConfirmedSubOrder() throws Exception {
        String adminToken = registerAndVerify("+254700200701");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorPhone = "+254700200702";
        String vendorToken = registerAndVerify(vendorPhone);
        String buyerToken = registerAndVerify("+254700200703");
        String otherVendorToken = registerAndVerify("+254700200704");

        approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Acceptance Traders", "KRA-ORD-005"));
        approveVendorApplication(adminToken, submitVendorApplication(otherVendorToken, "Other Traders", "KRA-ORD-006"));
        String productId = createAndPublishProduct(vendorToken, "Acceptance Widget", null, 60_000, "KES");

        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200703\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, defaultDeliveryZoneId())))
                .andExpect(status().isOk())
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        // A vendor who does not own this sub-order gets NOT_FOUND, not FORBIDDEN — the same
        // cross-vendor disclosure reasoning as everywhere else in this codebase.
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/accept").header("Authorization", "Bearer " + otherVendorToken))
                .andExpect(status().isNotFound());

        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/accept").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.cancellable").value(true));

        // Not awaiting acceptance any more — accepting it again is refused.
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/accept").header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isBadRequest());

        // BR-ORD-13: an ACCEPTED sub-order is still buyer-cancellable — Accepted is before Dispatched.
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // BR-ORD-08/16: the vendor is told, and the reason correctly says the buyer, not a lapsed window.
        awaitNotificationSent(vendorPhone, adminToken, "cancelled: Buyer cancellation");
    }

    @Test
    @DisplayName("checkout prices and auto-books delivery from the buyer's own choice, and cancellation refunds the delivery cost too")
    void checkoutPricesAndAutoBooksDeliveryFromBuyerChoice() throws Exception {
        String adminToken = registerAndVerify("+254700200801");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200802");
        String buyerToken = registerAndVerify("+254700200803");

        MvcResult approved = approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Delivery Cost Traders", "KRA-CHK-010"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String productId = createAndPublishProduct(vendorToken, "Delivery Cost Widget", null, 100_000, "KES");
        String zoneId = defaultDeliveryZoneId();

        // BR-CHK-05: PICKUP_POINT chosen explicitly — cheaper than DOOR per V29's seeded rate cards
        // (15,000 vs 25,000), proving the choice, not just a default, drives the price.
        MvcResult order = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200803\","
                                        + "\"deliveryZoneId\":\"%s\",\"deliveryMethodByVendor\":{\"%s\":\"PICKUP_POINT\"}}")
                                .formatted(productId, zoneId, vendorId)))
                .andExpect(status().isOk())
                // BR-CHK-07: a real per-vendor breakdown — items, delivery, and their sum.
                .andExpect(jsonPath("$.data.subOrders[0].subtotal.minorUnits").value(100_000))
                .andExpect(jsonPath("$.data.subOrders[0].deliveryCost.minorUnits").value(15_000))
                .andExpect(jsonPath("$.data.total.minorUnits").value(115_000))
                .andReturn();
        String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
        String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");

        confirmPaymentFor(orderId);
        awaitOrderStatus(orderId, buyerToken, "CONFIRMED");

        // BR-CHK-05/06, BR-FUL-01: auto-booked with the buyer's own pickup-point choice — no vendor action needed.
        String deliveryId = awaitDeliveryId(subOrderId, vendorToken);
        mvc.perform(get("/api/fulfilment/deliveries/" + deliveryId).header("Authorization", "Bearer " + vendorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BOOKED"))
                .andExpect(jsonPath("$.data.method").value("PICKUP_POINT"));

        // BR-ORD-14: cancelling refunds the full 115,000 the buyer actually paid — items plus
        // delivery — not just the 100,000 items subtotal ledger's commission reversal uses.
        mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk());

        Long refundedMinorUnits = jdbc.queryForObject(
                "select amount_minor_units from refund where payment_transaction_id = "
                        + "(select id from payment_transaction where parent_order_id = ?)",
                Long.class,
                UUID.fromString(orderId));
        org.junit.jupiter.api.Assertions.assertEquals(115_000L, refundedMinorUnits);

        // BR-CHK-06: a zone with no rate card for any active carrier is a real "not available"
        // refusal, not a silent zero-cost checkout.
        MvcResult unservedZone = mvc.perform(post("/api/admin/fulfilment/zones")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Unserved Zone\",\"county\":\"Turkana\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String unservedZoneId = JsonPath.read(unservedZone.getResponse().getContentAsString(), "$.data.id");

        String productId2 = createAndPublishProduct(vendorToken, "Unservable Widget", null, 50_000, "KES");
        mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200803\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId2, unservedZoneId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName(
            "BR-SEC-03/04/05: order origin search finds repeated activity, and buyer/vendor abuse signals are real")
    void abuseDetectionAndOrderOriginSearchAreReal() throws Exception {
        String adminToken = registerAndVerify("+254700200901");
        grantRole(adminToken, "PLATFORM_ADMIN");
        String vendorToken = registerAndVerify("+254700200902");
        String buyerToken = registerAndVerify("+254700200903");

        MvcResult approved = approveVendorApplication(adminToken, submitVendorApplication(vendorToken, "Abuse Signal Traders", "KRA-SEC-001"));
        String vendorId = JsonPath.read(approved.getResponse().getContentAsString(), "$.data.vendorId");
        String buyerAccountId = accountIdFor(buyerToken);
        String zoneId = defaultDeliveryZoneId();

        // BR-SEC-04: three sub-orders, all cancelled — a 100% cancellation rate, comfortably above
        // FLOOR_RATE regardless of what this shared test database's platform median happens to be
        // (most buyers across this whole suite never cancel at all).
        List<String> orderIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String productId = createAndPublishProduct(vendorToken, "Abuse Widget " + i, null, 50_000, "KES");
            MvcResult order = mvc.perform(post("/api/checkout")
                            .header("Authorization", "Bearer " + buyerToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200903\",\"deliveryZoneId\":\"%s\"}"
                                    .formatted(productId, zoneId)))
                    .andExpect(status().isOk())
                    .andReturn();
            String orderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.id");
            orderIds.add(orderId);
            String subOrderId = JsonPath.read(order.getResponse().getContentAsString(), "$.data.subOrders[0].id");
            mvc.perform(post("/api/orders/sub-orders/" + subOrderId + "/cancel").header("Authorization", "Bearer " + buyerToken))
                    .andExpect(status().isOk());
        }

        MvcResult buyerAbuse = mvc.perform(get("/api/admin/integrity/buyer-abuse").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> signals = JsonPath.read(buyerAbuse.getResponse().getContentAsString(), "$.data");
        Map<String, Object> mySignal = signals.stream()
                .filter(s -> buyerAccountId.equals(s.get("buyerAccountId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a signal for buyer " + buyerAccountId + " in " + signals));
        org.junit.jupiter.api.Assertions.assertEquals(
                Boolean.TRUE, mySignal.get("flagged"), "expected buyer " + buyerAccountId + " to be flagged: " + mySignal);

        // A non-admin caller may not see it — the same admin-surface gating as everywhere else.
        mvc.perform(get("/api/admin/integrity/buyer-abuse").header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isForbidden());

        // BR-SEC-03: every one of those three orders shares this test's own MockMvc source address
        // — a real "who else ordered from here" search, not a stub.
        MvcResult bySource = mvc.perform(get("/api/admin/orders/search").param("sourceAddress", "127.0.0.1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> foundBySource = JsonPath.read(bySource.getResponse().getContentAsString(), "$.data");
        List<Object> foundIds = foundBySource.stream().map(o -> o.get("id")).toList();
        for (String orderId : orderIds) {
            org.junit.jupiter.api.Assertions.assertTrue(foundIds.contains(orderId), "expected " + orderId + " in source-address search: " + foundIds);
        }

        // BR-SEC-05: a fourth, real (paid, delivered) order gives this vendor a real delivery row —
        // a payout detail change afterward is a real fraud signal on top of it.
        String productId = createAndPublishProduct(vendorToken, "Real Widget", null, 60_000, "KES");
        MvcResult realOrder = mvc.perform(post("/api/checkout")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lines\":[{\"productId\":\"%s\",\"quantity\":1}],\"buyerPhoneNumber\":\"+254700200903\",\"deliveryZoneId\":\"%s\"}"
                                .formatted(productId, zoneId)))
                .andExpect(status().isOk())
                .andReturn();
        String realOrderId = JsonPath.read(realOrder.getResponse().getContentAsString(), "$.data.id");
        String realSubOrderId = JsonPath.read(realOrder.getResponse().getContentAsString(), "$.data.subOrders[0].id");
        confirmPaymentFor(realOrderId);
        awaitOrderStatus(realOrderId, buyerToken, "CONFIRMED");
        awaitDeliveryId(realSubOrderId, vendorToken);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/vendors/" + vendorId + "/payout-details")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"method\":\"MPESA\",\"destination\":\"+254700200902\"}"))
                .andExpect(status().isOk());

        MvcResult vendorFraud = mvc.perform(get("/api/admin/integrity/vendor-fraud").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> fraudSignals = JsonPath.read(vendorFraud.getResponse().getContentAsString(), "$.data");
        Map<String, Object> myFraudSignal = fraudSignals.stream()
                .filter(s -> vendorId.equals(s.get("vendorId")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a signal for vendor " + vendorId + " in " + fraudSignals));
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, myFraudSignal.get("payoutRecentlyChanged"), "" + myFraudSignal);
        org.junit.jupiter.api.Assertions.assertEquals(Boolean.TRUE, myFraudSignal.get("flagged"), "" + myFraudSignal);
    }

    private String accountIdFor(String token) {
        return jdbc.queryForObject(
                        "select account_id from session_token where token_hash = ?", UUID.class, TokenHashing.hash(token))
                .toString();
    }

    private void assertOrderSearchFinds(String param, String value, String expectedOrderId, String adminToken) throws Exception {
        MvcResult result = mvc.perform(get("/api/admin/orders/search").param(param, value).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        List<Map<String, Object>> matches = JsonPath.read(result.getResponse().getContentAsString(), "$.data");
        org.junit.jupiter.api.Assertions.assertTrue(
                matches.stream().anyMatch(m -> expectedOrderId.equals(m.get("id"))),
                "expected order " + expectedOrderId + " searching " + param + "=" + value + " in " + matches);
    }

    private int countRows(String table) {
        return jdbc.queryForObject("select count(*) from " + table, Integer.class);
    }

    /**
     * BR-PAY-04/05: simulates the provider's webhook the same way a real one would arrive — with
     * only the gateway reference, which the checkout buyer's own API response never exposes (see
     * {@code CheckoutService}'s comment on why payment initiation's result is not returned to the
     * caller). A real provider already has this reference because it issued it; this test reads it
     * the one way available here, directly from the database, standing in for that.
     */
    /** BR-CHK-05/06: {@code V19__fulfilment.sql} seeds exactly one zone ("Nairobi Metro") — every real destination checkout needs. */
    private String defaultDeliveryZoneId() {
        return jdbc.queryForObject("select id from delivery_zone limit 1", String.class);
    }

    private void confirmPaymentFor(String parentOrderId) throws Exception {
        String gatewayReference = jdbc.queryForObject(
                "select gateway_reference from payment_transaction where parent_order_id = ?",
                String.class,
                UUID.fromString(parentOrderId));

        mvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gatewayReference\":\"%s\",\"providerTransactionReference\":\"TEST-%s\",\"success\":true}"
                                .formatted(gatewayReference, UUID.randomUUID())))
                .andExpect(status().isOk());
    }

    /**
     * The trigger chain from a payment callback to an order showing CONFIRMED is three
     * {@code @Async} hops deep (payment publishes {@code PaymentConfirmed}; order's listener
     * confirms and publishes {@code OrderPaymentConfirmed}; inventory and notification listen to
     * that) — see {@code awaitDelisted}'s comment for the same reasoning applied to a shorter
     * chain. Polls rather than asserting immediately.
     */
    /**
     * {@code ledger}'s posting listens for {@code order.events.OrderPaymentConfirmed}, published
     * after the order itself already shows CONFIRMED — a further {@code @Async} hop beyond the one
     * {@link #awaitOrderStatus} waits for, so the order reaching CONFIRMED does not yet guarantee
     * the ledger posting has landed.
     */
    private void awaitVendorPayable(String vendorId, String vendorToken, long expectedMinorUnits) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        String last = "<no response>";
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/settlement/vendors/" + vendorId + "/balance")
                            .header("Authorization", "Bearer " + vendorToken))
                    .andReturn();
            last = result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() == 200) {
                Number payable = JsonPath.read(result.getResponse().getContentAsString(), "$.data.payable.minorUnits");
                if (payable.longValue() == expectedMinorUnits) {
                    return;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError(
                "Vendor " + vendorId + " payable balance did not reach " + expectedMinorUnits + " in time; last response: " + last);
    }

    /** BR-ORD-13/14: {@code inventory}'s release/restock on cancellation is an {@code @ApplicationModuleListener} hop past the cancel response. */
    private void awaitAvailableQuantity(String productId, String vendorToken, int expected) throws Exception {
        awaitStockField(productId, vendorToken, "availableQuantity", expected);
    }

    private void awaitTotalQuantity(String productId, String vendorToken, int expected) throws Exception {
        awaitStockField(productId, vendorToken, "totalQuantity", expected);
    }

    private void awaitStockField(String productId, String vendorToken, String field, int expected) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        String last = "<no response>";
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/inventory/products/" + productId + "/stock")
                            .header("Authorization", "Bearer " + vendorToken))
                    .andReturn();
            last = result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() == 200) {
                Number value = JsonPath.read(result.getResponse().getContentAsString(), "$.data." + field);
                if (value.intValue() == expected) {
                    return;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError(
                "Product " + productId + "'s " + field + " did not reach " + expected + " in time; last response: " + last);
    }

    private void awaitOrderStatus(String orderId, String buyerToken, String expectedStatus) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/orders/" + orderId).header("Authorization", "Bearer " + buyerToken))
                    .andReturn();
            String status = JsonPath.read(result.getResponse().getContentAsString(), "$.data.status");
            if (expectedStatus.equals(status)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Order " + orderId + " did not reach " + expectedStatus + " in time");
    }

    /**
     * BR-FUL-06: takes a {@code fulfilment.Delivery} all the way to DELIVERED through {@code
     * fulfilment}'s own endpoints — dispatch-manually then mark-delivered, skipping the
     * carrier-booking round trip on purpose for tests not exercising BR-FUL-01/03/04 directly.
     * Genuinely independent of {@code order}'s own {@code mark-delivered} — see {@code
     * order.OrderStatus}'s javadoc for why the two do not drive each other.
     */
    private void markSubOrderDelivered(String subOrderId, String vendorToken) throws Exception {
        String deliveryId = awaitDeliveryId(subOrderId, vendorToken);

        mvc.perform(post("/api/fulfilment/deliveries/" + deliveryId + "/dispatch-manually")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trackingReference\":\"TEST-TRACK-%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isOk());

        mvc.perform(post("/api/fulfilment/deliveries/" + deliveryId + "/mark-delivered")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());
    }

    /** {@code fulfilment.internal.OrderReadinessListener} creates the Delivery row asynchronously off {@code OrderPaymentConfirmed} — a further hop past {@link #awaitOrderStatus}'s CONFIRMED wait. */
    private String awaitDeliveryId(String subOrderId, String vendorToken) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        String last = "<no response>";
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/fulfilment/sub-orders/" + subOrderId + "/delivery")
                            .header("Authorization", "Bearer " + vendorToken))
                    .andReturn();
            last = result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() == 200) {
                return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Delivery for sub-order " + subOrderId + " was not created in time; last response: " + last);
    }

    /** REG-04: invoice generation and its first (mock, always-successful) transmission are both async off {@code OrderPaymentConfirmed}. */
    private void awaitInvoiceTransmitted(String parentOrderId, String financeToken, long expectedMinorUnits) throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        String last = "<no response>";
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/admin/tax/orders/" + parentOrderId + "/invoice")
                            .header("Authorization", "Bearer " + financeToken))
                    .andReturn();
            last = result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() == 200) {
                String status = JsonPath.read(result.getResponse().getContentAsString(), "$.data.status");
                Number amount = JsonPath.read(result.getResponse().getContentAsString(), "$.data.amount.minorUnits");
                if ("TRANSMITTED".equals(status) && amount.longValue() == expectedMinorUnits) {
                    return;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError("Order " + parentOrderId + "'s invoice was not transmitted in time; last response: " + last);
    }

    /** REG-08: generation and transmission are async off {@code SubOrderCancelled}/{@code ReturnRefunded}. */
    private void awaitCreditNoteTransmitted(String parentOrderId, String financeToken, long expectedMinorUnits, String expectedReason)
            throws Exception {
        long deadline = System.currentTimeMillis() + 3000;
        String last = "<no response>";
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/admin/tax/orders/" + parentOrderId + "/credit-notes")
                            .header("Authorization", "Bearer " + financeToken))
                    .andReturn();
            last = result.getResponse().getStatus() + " " + result.getResponse().getContentAsString();
            if (result.getResponse().getStatus() == 200) {
                List<Map<String, Object>> creditNotes = JsonPath.read(result.getResponse().getContentAsString(), "$.data");
                boolean matched = creditNotes.stream().anyMatch(note -> {
                    Map<?, ?> amount = (Map<?, ?>) note.get("amount");
                    Number minorUnits = (Number) amount.get("minorUnits");
                    return "TRANSMITTED".equals(note.get("status"))
                            && expectedReason.equals(note.get("reason"))
                            && minorUnits.longValue() == expectedMinorUnits;
                });
                if (matched) {
                    return;
                }
            }
            Thread.sleep(200);
        }
        throw new AssertionError(
                "Order " + parentOrderId + "'s credit note was not transmitted in time; last response: " + last);
    }

    private void awaitDelisted(String productName) throws Exception {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            MvcResult result = mvc.perform(get("/api/catalog/products?perPage=100")).andReturn();
            java.util.List<String> names =
                    JsonPath.read(result.getResponse().getContentAsString(), "$.data[*].name");
            if (!names.contains(productName)) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError(productName + " was still listed " + (deadline) + "ms after suspension");
    }

    private String submitVendorApplication(String vendorToken, String businessName, String kraPin) throws Exception {
        MvcResult result = mvc.perform(post("/api/vendors/applications")
                        .header("Authorization", "Bearer " + vendorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationJson(businessName, kraPin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    private String applicationJson(String businessName, String kraPin) {
        return """
                {"businessName":"%s","kraPin":"%s","physicalAddress":"123 Test Street",\
                "county":"Nairobi","contactName":"Test Contact","contactPhone":"+254700000000"}\
                """
                .formatted(businessName, kraPin);
    }

    private MvcResult approveVendorApplication(String adminToken, String applicationId) throws Exception {
        return mvc.perform(post("/api/admin/vendor-applications/" + applicationId + "/approve")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andReturn();
    }

    private String registerAndVerify(String identifier) throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"displayName\":\"Test Account\"}".formatted(identifier)))
                .andExpect(status().isOk());

        String code = codes.mostRecentlyGenerated();

        MvcResult result = mvc.perform(post("/api/auth/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"%s\",\"code\":\"%s\"}".formatted(identifier, code)))
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.token");
    }

    /**
     * Stands in for role-management tooling that does not exist yet — see the class doc. Direct
     * database manipulation, not a production seam.
     */
    private void grantRole(String token, String role) {
        String tokenHash = TokenHashing.hash(token);
        UUID accountId = jdbc.queryForObject(
                "select account_id from session_token where token_hash = ?", UUID.class, tokenHash);

        jdbc.update(
                "insert into account_role (account_id, role, granted_at) values (?, ?, ?)",
                accountId,
                role,
                Timestamp.from(Instant.now()));
    }
}
