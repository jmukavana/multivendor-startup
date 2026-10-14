/**
 * Cart and Checkout.
 *
 * <p>Implements BR-CHK of the business requirements document. Delivered in phase 4, with
 * BR-CHK-05/06/07 (delivery method selection, cost calculation, cost breakdown) completed in
 * phase 9 — {@code fulfilment} was already in {@code allowedDependencies} from the start,
 * anticipating exactly this; only the wiring was deferred.
 *
 * <p><strong>BR-CHK-08's saved address book is still not built.</strong> The buyer supplies one
 * {@code deliveryZoneId} per checkout call instead of choosing from a saved list — the same
 * "submitted every call, not persisted" treatment BR-CHK-03's cart already gets, and a real,
 * if scaled-down, stand-in rather than a full address model. One zone for the whole order, not
 * per vendor group: {@code fulfilment.DeliveryRateCard} prices by destination zone and method
 * only, not origin (BR-FUL's own Phase 7 build never modelled per-vendor origin pricing), so a
 * single destination is what the priced data actually supports — {@code deliveryMethodByVendor}
 * is the dimension that genuinely does vary per vendor group, and does.
 *
 * <p><strong>Delivery cost flows into {@code order} as money only, never as a zone or method.</strong>
 * {@code order.OrderCreation.create} takes a {@code Map<UUID, Money>} keyed by vendor id —
 * {@code order} does not depend on {@code fulfilment} and never will just to price a sub-order; it
 * only needs to know each vendor group owes this much. The zone, method, carrier and rate card
 * that produced that figure are recorded separately, straight into {@code fulfilment} via {@link
 * com.jamlapps.meridian.fulfilment.DeliverySelections#select}, keyed by the sub-order's own id
 * once {@code order.OrderCreation} has returned it — see that interface's javadoc for why this,
 * not a field on {@code OrderPaymentConfirmed}, is how {@code fulfilment} learns the buyer's
 * choice once payment confirms.
 *
 * <p><strong>A quote is resolved once and never re-resolved</strong> — the same versioned-value
 * discipline {@code order} already applies to the commission rate it captures at creation (COM-02,
 * DAT-07). {@code fulfilment.DeliveryQuotes.cheapestQuote} runs before {@code order.OrderCreation
 * .create}, and the exact carrier, rate card and price it returns are what gets charged and what
 * later gets booked — never a fresh lookup against whatever rate happens to be current by the time
 * payment confirms.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Cart and Checkout",
        allowedDependencies = {"catalog", "inventory", "vendor", "pricing", "fulfilment", "order", "payment", "web"})
package com.jamlapps.meridian.checkout;
