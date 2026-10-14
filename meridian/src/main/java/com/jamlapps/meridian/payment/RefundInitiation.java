package com.jamlapps.meridian.payment;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

/**
 * BR-PAY-11: refund initiation through the provider, "with full traceability to the original
 * transaction" — resolved from {@code parentOrderId} to the payment transaction that paid for it,
 * so a caller (only {@code returns}, currently) never needs to know a {@code paymentTransactionId}
 * exists. Takes {@code amount} rather than deriving it, the same reason {@link PaymentInitiation}
 * does: {@code payment} has no dependency that could compute a refund amount itself (that is
 * BR-RTN-04's job, in a module {@code payment} does not and should not depend on), so the caller —
 * which already resolved it — supplies it directly.
 */
public interface RefundInitiation {

    Refund initiate(UUID parentOrderId, Money amount, String reason);
}
