package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

/**
 * ADR-006: the disbursement side of the same aggregator {@code payment.internal.PaymentGateway}
 * abstracts the collection side of — a separate interface because a real provider's B2C
 * (disbursement) and C2B (collection) APIs are typically different endpoints with different
 * failure modes, not because the underlying aggregator is expected to differ.
 *
 * <p>Deliberately does not receive the vendor's actual payout destination (M-Pesa number or bank
 * account). That value lives in {@code vendor.internal.PayoutDetailEntity} and is treated there as
 * sensitive-adjacent — see {@code PayoutDetailService}'s own comment on why its read model never
 * returns the raw destination, even inside {@code vendor} itself. A real integration will need
 * {@code vendor} to expose a narrowly-scoped resolution capability for disbursement specifically;
 * until then this mock resolves nothing and simply records that vendorId as the recipient, which is
 * enough to exercise BR-STL-06/07/12's flow without punching a hole through that boundary now.
 */
interface DisbursementGateway {

    /** BR-STL-06/12: instructs disbursement, tagged with the idempotency key BR-STL-12 requires. */
    GatewayDisbursement disburse(UUID vendorId, Money amount, String idempotencyKey);

    record GatewayDisbursement(String disbursementReference) {}
}
