/**
 * Settlement and Payouts.
 *
 * <p>Implements BR-STL of the business requirements document. Delivered in phase 6.
 * Allowed dependencies: ledger, vendor, order, returns, pricing.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Settlement and Payouts",
        allowedDependencies = { "ledger", "vendor", "order", "returns", "pricing" })
package com.jamlapps.meridian.settlement;
