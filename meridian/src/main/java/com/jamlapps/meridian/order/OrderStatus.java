package com.jamlapps.meridian.order;

/**
 * BR-ORD-01/04 through the manual payment-marking step described in BRD 18's Phase 1 exit
 * criterion, plus DELIVERED (Phase 5, added for BR-RTN-01's return window).
 *
 * <p>BR-ORD-03's full lifecycle — Accepted, Dispatched, In Transit, Delivered, Returned — is still
 * not fully here, and stays that way even with Phase 7's {@code fulfilment} module built.
 * {@code fulfilment} owns a genuinely richer {@code Delivery.status} of its own
 * (PENDING/BOOKED/DISPATCHED/IN_TRANSIT/DELIVERED/FAILED — BRD 15.1 lists Delivery under the
 * Fulfilment module, not Orders), but it cannot drive DELIVERED here: {@code fulfilment} depends
 * on {@code order::events} one-directionally (ADR-002), and the reverse — {@code order} depending
 * on {@code fulfilment::events} to consume its completion — would close a module dependency
 * cycle. {@code ModularityTests} refused exactly that when it was tried; see {@code order
 * .package-info}'s javadoc for the concrete violation.
 *
 * <p>{@code OrderService.markDelivered} therefore stays vendor-invoked and ownership-checked, the
 * same manual trigger it was before {@code fulfilment} existed — a genuinely separate "delivered"
 * from {@code fulfilment}'s own, not one superseding the other. Unifying the two — extending this
 * enum to mirror {@code fulfilment}'s lifecycle, or restructuring the dependency graph so one can
 * drive the other — is real future work this pass does not attempt, not a gap it hides: it is the
 * kind of change ADR-002 itself says must be a deliberate, documented architectural decision, and
 * a plausible direction is a mediating module (matching {@code checkout}'s existing dependency on
 * both {@code order} and {@code fulfilment}) rather than a direct edge between the two.
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    CONFIRMED,

    /**
     * BR-ORD-03/05: a sub-order-only value — no parent order is ever "accepted", only its
     * sub-orders individually are, by the vendor behind each one. A parent order's own {@code
     * status} field stays whatever {@link com.jamlapps.meridian.order.internal.OrderService
     * #markPaymentConfirmed} set it to; BR-ORD-04's "derive parent status from the aggregate of
     * its sub-orders" was never fully implemented even before this value existed — {@code cancel}
     * and {@code markDelivered} already moved only a sub-order's own status, leaving the parent's
     * frozen at CONFIRMED, so this follows the same, already-established precedent rather than
     * introducing a new one.
     */
    ACCEPTED,

    DELIVERED,
    CANCELLED
}
