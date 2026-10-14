/**
 * Ledger.
 *
 * <p>Implements BR-LDG of the business requirements document. Delivered in phase 3.
 * Allowed dependencies: payment::events, order::events, returns::events.
 *
 * <p><strong>The holdback rate {@code LedgerService} applies is real, versioned configuration as
 * of BR-ADM-06's closing pass — no longer a bare {@code BigDecimal("0.10")} constant.</strong>
 * {@code order} resolves {@code pricing.PricingConfiguration.currentHoldbackRate()} once, at
 * order creation (the same COM-05 discipline it already applied to commission rate), and carries
 * the resolved rate — never "the current rate" — on {@code OrderPaymentConfirmed}, {@code
 * SubOrderCancelled} and {@code ReturnRefunded}. {@code ledger} still depends only on those three
 * modules' {@code ::events}, exactly as before: the versioning work happened entirely upstream,
 * in {@code order}, {@code returns} and {@code pricing}, and this module's own boundary did not
 * need to move to receive it.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Ledger",
        allowedDependencies = { "payment::events", "order::events", "returns::events" })
package com.jamlapps.meridian.ledger;
