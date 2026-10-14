package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for DigiTax — an OSCU/VSCU-certified eTIMS integration facilitator (INT-02, DEP-04:
 * "eTIMS onboarding and credentials", still pending) — rather than a hypothetical, unnamed
 * aggregator, matching every other mock provider's "at least one, real when onboarding completes"
 * reasoning. Named specifically because DigiTax is the intended integration route, not an
 * arbitrary placeholder; the real integration, once DEP-04 closes, replaces only this class.
 *
 * <p>Always succeeds immediately — there is no failure path to exercise REG-05's retry against
 * until either DigiTax's real API or a deliberately-failing test double replaces this, the same
 * limitation every other mock gateway in this codebase already carries.
 */
@Component
class MockDigiTaxGateway implements EtimsGateway {

    private static final Logger log = LoggerFactory.getLogger(MockDigiTaxGateway.class);

    @Override
    public GatewayTransmission transmitInvoice(String invoiceNumber, Money amount) {
        String reference = "MOCK-DIGITAX-INV-" + UUID.randomUUID();
        log.info(
                "[DEV ONLY — no DigiTax/eTIMS integration configured] Invoice {} for {} transmitted ({})",
                invoiceNumber,
                amount,
                reference);
        return new GatewayTransmission(true, reference);
    }

    @Override
    public GatewayTransmission transmitCreditNote(String invoiceNumber, Money amount) {
        String reference = "MOCK-DIGITAX-CN-" + UUID.randomUUID();
        log.info(
                "[DEV ONLY — no DigiTax/eTIMS integration configured] Credit note against invoice {} for {} transmitted ({})",
                invoiceNumber,
                amount,
                reference);
        return new GatewayTransmission(true, reference);
    }
}
