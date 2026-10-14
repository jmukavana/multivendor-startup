package com.jamlapps.meridian.fulfilment.internal;

import java.util.UUID;

/** ADR-006, matching {@code payment.internal.PaymentGateway}'s reasoning exactly — the real carrier's booking/dispatch API, in this module's own vocabulary. */
interface CarrierGateway {

    /** BR-FUL-01: books a carrier for one delivery. */
    GatewayBooking book(UUID deliveryId, UUID carrierId);

    /** BR-FUL-04: dispatches an already-booked delivery, returning a tracking reference. */
    GatewayDispatch dispatch(UUID deliveryId, UUID carrierId);

    record GatewayBooking(boolean accepted, String carrierReference) {}

    record GatewayDispatch(String trackingReference) {}
}
