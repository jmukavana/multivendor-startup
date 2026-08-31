/**
 * Fulfilment and Logistics.
 *
 * <p>Implements BR-FUL of the business requirements document. Delivered in phase 7.
 * Allowed dependencies: vendor, order::events.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Fulfilment and Logistics",
        allowedDependencies = { "vendor", "order::events" })
package com.jamlapps.meridian.fulfilment;
