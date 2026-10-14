package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real aggregator's B2C API (OPEN-03), matching {@code payment.internal
 * .MockPaymentGateway}'s reasoning exactly. Always succeeds immediately; there is no failure path
 * to exercise BR-STL-07's retry against until a real provider — or a deliberately-failing test
 * double — replaces this.
 */
@Component
class MockDisbursementGateway implements DisbursementGateway {

    private static final Logger log = LoggerFactory.getLogger(MockDisbursementGateway.class);

    @Override
    public GatewayDisbursement disburse(UUID vendorId, Money amount, String idempotencyKey) {
        String reference = "MOCK-DISB-" + UUID.randomUUID();
        log.info(
                "[DEV ONLY — no disbursement aggregator configured] Disbursing {} to vendor {} (idempotency key {}, reference {})",
                amount,
                vendorId,
                idempotencyKey,
                reference);
        return new GatewayDisbursement(reference);
    }
}
