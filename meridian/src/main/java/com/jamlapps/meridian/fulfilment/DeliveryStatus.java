package com.jamlapps.meridian.fulfilment;

/** BR-FUL-01/04/06. */
public enum DeliveryStatus {

    /** Eligible for booking — the sub-order behind it is CONFIRMED, nothing engaged yet. */
    PENDING,

    /** BR-FUL-01/03: a carrier has been engaged (or, for the manual path, a vendor has taken it on). */
    BOOKED,

    /** BR-FUL-04: dispatched, with a tracking reference. */
    DISPATCHED,

    /** AC-FUL-02: for PICKUP_POINT, this is where the parcel sits at the agent point awaiting collection. */
    IN_TRANSIT,

    /** BR-FUL-06/07: delivered, optionally with proof. */
    DELIVERED,

    /** AC-FUL-04: a carrier booking failed; a manual dispatch option is available from here. */
    FAILED
}
