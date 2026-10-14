package com.jamlapps.meridian.inventory;

public enum ReservationOutcome {
    RESERVED,
    CONFIRMED,
    RELEASED,

    /** BR-ORD-13/14: a CONFIRMED reservation given back by a buyer cancellation — see {@code StockMovementEntity.Reason.CANCELLED}'s javadoc for why this is distinct from RELEASED. */
    CANCELLED
}
