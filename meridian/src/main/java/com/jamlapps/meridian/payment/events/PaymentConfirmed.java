package com.jamlapps.meridian.payment.events;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;

/**
 * BR-PAY-04/10: the only trigger that may move an order past Pending Payment. {@code order}
 * listens and calls its own {@code markPaymentConfirmed} — see that method's javadoc for why the
 * old HTTP path to it is retired now that this exists.
 */
public record PaymentConfirmed(
        UUID paymentTransactionId, UUID parentOrderId, Money amount, String providerTransactionReference, Instant confirmedAt) {}
