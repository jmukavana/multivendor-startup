/**
 * Returns, refunds and disputes.
 *
 * <p>Implements BR-RTN of the business requirements document. Delivered in phase 5.
 * Allowed dependencies: order, payment, vendor.
 *
 * <p>ADR-002's table lists only {@code order} and {@code payment} here. {@code vendor} is a
 * deliberate addition, for the same reason {@code settlement} needed it and {@code ledger} could
 * not have it: BR-RTN-03's vendor accept/reject action needs to verify the caller owns the vendor
 * on the return, and the only honest way to do that is to ask {@code vendor} directly — routing it
 * through an ad-hoc method on {@code order} would misplace vendor-ownership authorization in a
 * module that does not own it, the exact anti-pattern the settlement/ledger arc's fix corrected.
 *
 * <p>{@code pricing} is deliberately absent even though a return needs a commission rate to reverse
 * (BR-RTN-05). {@code order.SubOrderDetail} already carries the resolved rate — {@code order}
 * resolved it via {@code pricing.rateForVersion} for its own {@code markPaymentConfirmed} — so
 * {@code returns} never needs to re-derive it, matching {@code OrderPaymentConfirmed
 * .SubOrderSettlementDetail}'s reasoning one module over.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Returns",
        allowedDependencies = { "order", "payment", "vendor" })
package com.jamlapps.meridian.returns;
