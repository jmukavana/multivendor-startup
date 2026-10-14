/**
 * Orders.
 *
 * <p>Implements BR-ORD of the business requirements document. Delivered in phase 1, 4 and 5.
 * Allowed dependencies: catalog, vendor, pricing, payment, payment::events.
 *
 * <p>{@code payment} (the full module, not just its events) was added for BR-ORD-14: buyer
 * cancellation calls {@code payment.RefundInitiation} synchronously, the same "these two things
 * succeed or fail together" reasoning as {@code checkout}'s {@code order.OrderCreation} call and
 * {@code settlement}'s {@code ledger.LedgerPosting} calls — {@code OrderService.cancel} must know
 * the refund succeeded before considering the cancellation itself resolved. {@code payment::events}
 * stays separately declared alongside it, matching {@code catalog}'s {@code vendor}/{@code
 * vendor::events} precedent: a full module dependency and its named-interface events package are
 * distinct grants in this codebase, not one subsuming the other.
 *
 * <p><b>Not depending on {@code fulfilment}, deliberately, even though Phase 7 built it.</b> The
 * first attempt at wiring {@code fulfilment}'s delivery-completion event back into this module's
 * own DELIVERED transition added {@code fulfilment::events} here — and {@code ModularityTests}
 * refused it: ADR-002 already has {@code fulfilment} depending on {@code order::events} one
 * directionally, and the reverse dependency would have closed a cycle. {@code
 * OrderService.markDelivered} therefore stays exactly what it was before {@code fulfilment}
 * existed — vendor-invoked, ownership-checked directly against {@code vendor} — and {@code
 * fulfilment}'s own, much richer delivery lifecycle stays genuinely independent of it. See {@code
 * OrderStatus}'s javadoc for the full reasoning.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Orders",
        allowedDependencies = {"catalog", "vendor", "pricing", "payment", "payment::events", "web", "shared"})
package com.jamlapps.meridian.order;
