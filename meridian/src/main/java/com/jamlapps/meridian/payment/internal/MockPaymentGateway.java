package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real CBK-licensed aggregator (OPEN-03). Initiation always succeeds and returns
 * immediately; nothing here ever confirms a payment — that only happens through
 * {@code PaymentController}'s callback endpoint, matching BR-PAY-04 exactly whether the caller is
 * a real provider webhook or (until one exists) a test simulating one. Logged at INFO, matching
 * this codebase's other DEV-ONLY provider stand-ins (see {@code OneTimeCodeNotificationListener}).
 */
@Component
class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    @Override
    public GatewayInitiation initiateCollection(Money amount, String buyerPhoneNumber, String merchantReference) {
        String gatewayReference = "MOCK-" + UUID.randomUUID();
        log.info(
                "[DEV ONLY — no payment aggregator configured] STK push simulated: {} to {} for order {} ({})",
                amount,
                buyerPhoneNumber,
                merchantReference,
                gatewayReference);
        return new GatewayInitiation(gatewayReference);
    }
}
