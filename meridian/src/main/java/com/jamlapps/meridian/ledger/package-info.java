/**
 * Ledger.
 *
 * <p>Implements BR-LDG of the business requirements document. Delivered in phase 3.
 * Allowed dependencies: payment::events, order::events, returns::events.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Ledger",
        allowedDependencies = { "payment::events", "order::events", "returns::events" })
package com.jamlapps.meridian.ledger;
