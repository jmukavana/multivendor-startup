package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.ledger.VendorLedgerBalance;
import com.jamlapps.meridian.settlement.SettlementCycle;
import com.jamlapps.meridian.settlement.SettlementLine;
import com.jamlapps.meridian.settlement.SettlementRun;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-STL: finance's settlement-run actions, plus the vendor-facing balance check that used to live
 * on {@code ledger.internal.LedgerController} before this module existed. That endpoint moved here
 * — not left as a parallel path — because {@code ledger} is not allowed to depend on {@code vendor}
 * (ADR-002) and the ownership check it needs cannot be done without that dependency; {@code
 * settlement} is exactly the module ADR-002 already names as allowed to depend on both.
 *
 * <p>Method-secured with {@code @PreAuthorize}, matching {@code AdminVendorApplicationController}'s
 * reasoning: these are {@code settlement}'s own state transitions, not generic back-office
 * plumbing, and the {@code admin} aggregation module does not exist yet.
 */
@RestController
@RequestMapping("/settlement")
class SettlementController {

    private final SettlementService settlement;
    private final Clock clock;

    SettlementController(SettlementService settlement, Clock clock) {
        this.settlement = settlement;
        this.clock = clock;
    }

    record InitiateRunRequest(@NotBlank(message = "must not be blank") String currency) {}

    record HoldRequest(@NotBlank(message = "a hold reason is required") String reason) {}

    record CreateCycleConfigurationRequest(
            @NotNull(message = "must be provided") @Min(value = 1, message = "must be at least 1")
                    @Max(value = 31, message = "must be at most 31")
                    Integer cycleDays,
            @Nullable Instant effectiveFrom) {}

    @PostMapping("/runs")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<SettlementRun> initiate(
            @Valid @RequestBody InitiateRunRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(settlement.initiate(Currency.getInstance(request.currency()), principal.accountId()));
    }

    @GetMapping("/runs/{runId}")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<SettlementRun> get(@PathVariable UUID runId) {
        return ApiResponse.of(settlement.get(runId));
    }

    @GetMapping("/runs/{runId}/lines")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<SettlementLine>> lines(@PathVariable UUID runId) {
        return ApiResponse.of(settlement.listLines(runId));
    }

    /**
     * BR-STL-06/11: approves the run, then attempts every PENDING line once, synchronously — the
     * same "acceptable simplification because the gateway is a mock" checkout's own payment call
     * documents. {@code disburseLine} is called here, on {@code settlement} (this bean's injected,
     * proxied collaborator), never looped from inside {@code SettlementService} itself — see that
     * class's doc for why that distinction matters, not just stylistically.
     */
    @PostMapping("/runs/{runId}/approve")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<SettlementRun> approve(@PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        settlement.approve(runId, principal.accountId());

        for (UUID lineId : settlement.pendingLineIds(runId)) {
            settlement.disburseLine(lineId);
        }

        return ApiResponse.of(settlement.get(runId));
    }

    @PostMapping("/vendors/{vendorId}/hold")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Void> hold(
            @PathVariable UUID vendorId, @Valid @RequestBody HoldRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        settlement.hold(vendorId, request.reason(), principal.accountId());
        return ApiResponse.of(null);
    }

    @PostMapping("/holds/{holdId}/release")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Void> releaseHold(@PathVariable UUID holdId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        settlement.releaseHold(holdId, principal.accountId());
        return ApiResponse.of(null);
    }

    /** BR-ADM-06: configuration management for the settlement cycle — see {@code admin.package-info} for why this lives here, not in {@code admin}. */
    @PostMapping("/config/cycle")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<SettlementCycle> createCycleConfiguration(
            @Valid @RequestBody CreateCycleConfigurationRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        Instant effectiveFrom = request.effectiveFrom() != null ? request.effectiveFrom() : clock.instant();
        return ApiResponse.of(settlement.createCycleConfiguration(request.cycleDays(), effectiveFrom, principal.accountId()));
    }

    @GetMapping("/vendors/{vendorId}/balance")
    ApiResponse<VendorLedgerBalance> balance(
            @PathVariable UUID vendorId,
            @RequestParam(defaultValue = "KES") String currency,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        boolean isFinance = principal.hasAuthority("ROLE_FINANCE_ADMIN") || principal.hasAuthority("ROLE_SUPER_ADMIN");
        return ApiResponse.of(
                settlement.balanceFor(vendorId, Currency.getInstance(currency), principal.accountId(), isFinance));
    }
}
