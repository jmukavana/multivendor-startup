package com.jamlapps.meridian.payment;

/**
 * BR-PAY-07. REVERSED exists for a confirmed payment refunded in full (chargeback-equivalent);
 * nothing sets it yet — BR-PAY-11's refunds (see {@link Refund}) are recorded as their own
 * append-only rows against a transaction that stays CONFIRMED, since BR-RTN-04 allows several
 * partial refunds against one transaction and nothing here tracks "refunded in full" yet.
 */
public enum PaymentStatus {
    INITIATED,
    AWAITING_CONFIRMATION,
    CONFIRMED,
    FAILED,
    TIMED_OUT,
    REVERSED
}
