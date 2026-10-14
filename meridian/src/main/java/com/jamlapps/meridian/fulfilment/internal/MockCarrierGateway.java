package com.jamlapps.meridian.fulfilment.internal;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Stands in for a real logistics API (OPEN-03's sibling for BR-FUL-01), matching {@code payment.internal.MockPaymentGateway}'s reasoning exactly. Always succeeds immediately — AC-FUL-04's failure path is exercised by a deliberately-failing test double, not this one, same as every other mock provider in this codebase. */
@Component
class MockCarrierGateway implements CarrierGateway {

    private static final Logger log = LoggerFactory.getLogger(MockCarrierGateway.class);

    @Override
    public GatewayBooking book(UUID deliveryId, UUID carrierId) {
        String reference = "MOCK-BOOKING-" + UUID.randomUUID();
        log.info("[DEV ONLY — no carrier integration configured] Booking simulated for delivery {} with carrier {} ({})", deliveryId, carrierId, reference);
        return new GatewayBooking(true, reference);
    }

    @Override
    public GatewayDispatch dispatch(UUID deliveryId, UUID carrierId) {
        String tracking = "MOCK-TRACK-" + UUID.randomUUID();
        log.info("[DEV ONLY — no carrier integration configured] Dispatch simulated for delivery {} with carrier {} ({})", deliveryId, carrierId, tracking);
        return new GatewayDispatch(tracking);
    }
}
