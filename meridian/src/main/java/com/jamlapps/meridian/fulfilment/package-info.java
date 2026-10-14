/**
 * Fulfilment and Logistics.
 *
 * <p>Implements BR-FUL of the business requirements document. Delivered in phase 7.
 * Allowed dependencies: vendor, order::events.
 *
 * <p>Matches ADR-002's table exactly — the first module this codebase has built where no
 * deviation was needed either way. {@code order::events} is listen-only, deliberately: this
 * module never calls into {@code order}. It learns a sub-order needs a delivery by listening for
 * {@code OrderPaymentConfirmed} (see {@code OrderReadinessListener}).
 *
 * <p>{@code fulfilment.events.DeliveryCompleted} is published on delivery completion (BR-FUL-06/07)
 * but has no consumer yet — the same "declare ahead of the consumer" pattern {@code vendor::events}
 * and {@code settlement::events} were built under. The first thing tried here was {@code order}
 * listening to it to drive its own DELIVERED transition; {@code order} already depends on {@code
 * vendor}, and this module already depends on {@code order::events}, so {@code order} depending
 * back on {@code fulfilment::events} would have closed a module cycle — {@code ModularityTests}
 * refused it. See {@code order.OrderStatus}'s javadoc for the full reasoning and what a real fix
 * would need (a mediating module, not a direct edge). {@code order}'s own DELIVERED stays driven
 * by its pre-existing vendor-invoked endpoint, genuinely independent of this module's richer
 * {@code Delivery.status}.
 *
 * <p><strong>Wired to {@code checkout}, completed in phase 9.</strong> {@code checkout} calls
 * {@link DeliveryQuotes} to price delivery per vendor group before an order exists, and {@link
 * DeliverySelections} to record the buyer's carrier, zone and method choice against each real
 * sub-order id right after — a {@code delivery_selection} row that outlives the checkout request
 * itself. Neither interface gives {@code checkout} a dependency on this module beyond what
 * ADR-002's table already granted it. {@code OrderReadinessListener} still creates one {@link
 * Delivery} row per confirmed sub-order exactly as before, but now reads that selection back and
 * auto-books immediately through {@code DeliveryService.autoBook} — no vendor action needed. A
 * sub-order with no selection (nothing else in this codebase creates an order any other way, but a
 * future path might) falls back to the original PENDING, unbooked behaviour, a vendor booking it
 * by hand through this module's own controller.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Fulfilment and Logistics",
        allowedDependencies = { "vendor", "order::events" })
package com.jamlapps.meridian.fulfilment;
