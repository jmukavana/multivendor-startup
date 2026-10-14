package com.jamlapps.meridian.order;

import com.jamlapps.meridian.shared.Money;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * BR-ORD-01/02: order creation as a cross-module API, called by {@code checkout} (Phase 4).
 *
 * <p>Separated from the read-only {@link Orders} interface deliberately — every other
 * cross-module lookup in this codebase ({@code Accounts}, {@code Vendors}, {@code Products}) is
 * read-only, and this is the first genuinely synchronous cross-module <em>write</em>. It exists as
 * a direct call rather than an event because {@code checkout} needs order creation and inventory
 * reservation to succeed or fail together in one transaction — an event's own transaction commits
 * separately (ADR-004), which would let an order exist with no stock behind it if reservation
 * failed afterward.
 */
public interface OrderCreation {

    /**
     * BR-ORD-02: splits {@code lines} into one sub-order per vendor. Revalidates each product's
     * price and availability against current catalog state — the caller's cart may be stale
     * (BR-CHK-04).
     *
     * @param deliveryCostByVendor BR-CHK-06/07: the delivery cost for each vendor group, already
     *     resolved by {@code checkout} against {@code fulfilment.DeliveryQuotes} before this call
     *     — {@code order} does not know what a delivery zone or method is (it depends on neither
     *     {@code fulfilment} nor its vocabulary), only that each vendor group owes this much for
     *     it. A vendor id absent from the map is charged {@link Money#zero} — every real caller
     *     supplies an entry for every vendor group; this is a default for a caller that does not,
     *     not a documented way to skip delivery pricing.
     * @param origin BR-SEC-03: recorded once, against the parent order, never touched again.
     */
    ParentOrder create(UUID buyerAccountId, List<OrderLineRequest> lines, Map<UUID, Money> deliveryCostByVendor, RequestOrigin origin);
}
