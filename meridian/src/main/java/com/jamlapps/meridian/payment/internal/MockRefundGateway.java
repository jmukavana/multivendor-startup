package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stands in for the real aggregator's refund API (OPEN-03), matching {@link MockPaymentGateway}'s reasoning exactly. Always succeeds immediately. */
@Component
class MockRefundGateway implements RefundGateway {

    private static final Logger log = LoggerFactory.getLogger(MockRefundGateway.class);

    @Override
    public GatewayRefund refund(Money amount, String originalGatewayReference, String reason) {
        String reference = "MOCK-REFUND-" + UUID.randomUUID();
        log.info(
                "[DEV ONLY — no payment aggregator configured] Refund simulated: {} against {} ({}), reason: {}",
                amount,
                originalGatewayReference,
                reference,
                reason);
        return new GatewayRefund(reference);
    }
}
