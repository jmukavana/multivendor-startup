/**
 * Pricing and Configuration.
 *
 * <p>Implements COM-01 to COM-05, DAT-07, DAT-08 of the business requirements document. Delivered in phase 0 and 3.
 * Depends on no other module.
 *
 * <p><strong>BR-ADM-06's holdback percentage joined commission rate here, closing a deferred
 * gap.</strong> {@code HoldbackRate} is the same versioned-value shape as {@code CommissionRate},
 * resolved and stored the same way ({@code order} calls {@link
 * PricingConfiguration#currentHoldbackRate} once, at sub-order creation, and stores the returned
 * version id) — see that interface's javadoc. This module's own boundary and dependency-free
 * status did not change; the value simply reuses the mechanism COM-05 already built. Settlement
 * cycle, the third BR-ADM-06 value, deliberately did not join it here — see {@code
 * settlement.SettlementCycle}'s javadoc for why it stays inside {@code settlement} instead.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Pricing and Configuration",
        allowedDependencies = {})
package com.jamlapps.meridian.pricing;
