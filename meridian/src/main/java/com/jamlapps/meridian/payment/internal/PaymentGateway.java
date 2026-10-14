package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.shared.Money;

/**
 * ADR-006: the aggregator behind an internal interface, in this module's own vocabulary — no
 * M-Pesa or Daraja type appears outside this package. {@link MockPaymentGateway} is the only
 * implementation until OPEN-03 closes and a real aggregator is qualified; it lets initiation and
 * the callback path be built and tested now rather than waiting on a provider contract.
 */
interface PaymentGateway {

    /** BR-PAY-02: initiates collection (an M-Pesa STK push, for the real provider). */
    GatewayInitiation initiateCollection(Money amount, String buyerPhoneNumber, String merchantReference);

    record GatewayInitiation(String gatewayReference) {}
}
