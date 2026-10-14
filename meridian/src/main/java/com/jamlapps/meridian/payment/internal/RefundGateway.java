package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.shared.Money;

/** ADR-006, matching {@link PaymentGateway} and {@code settlement.internal.DisbursementGateway}'s reasoning exactly — a third aggregator-abstraction interface, not a third design. */
interface RefundGateway {

    /** BR-PAY-11. */
    GatewayRefund refund(Money amount, String originalGatewayReference, String reason);

    record GatewayRefund(String providerRefundReference) {}
}
