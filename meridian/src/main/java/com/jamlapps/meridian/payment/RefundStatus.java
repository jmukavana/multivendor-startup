package com.jamlapps.meridian.payment;

/** BR-PAY-11. {@code MockRefundGateway} always resolves to CONFIRMED immediately, matching every other mock provider in this codebase. */
public enum RefundStatus {
    INITIATED,
    CONFIRMED,
    FAILED
}
