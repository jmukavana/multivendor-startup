package com.jamlapps.meridian.returns;

/** BR-RTN-03: a return's outcome. There is no separate "vendor accepted, awaiting refund" state — refund initiation is synchronous (see {@code ReturnService}), so acceptance and resolution happen in the same call. */
public enum ReturnStatus {

    /** Awaiting the vendor's response, or a timeout. */
    REQUESTED,

    /** BR-RTN-03: vendor rejected or the response window elapsed; a {@link Dispute} now exists. */
    ESCALATED,

    /** Refunded — vendor accepted, or an admin resolved the escalation in the buyer's favour. */
    RESOLVED_BUYER,

    /** Not refunded — an admin resolved the escalation in the vendor's favour. */
    RESOLVED_VENDOR
}
