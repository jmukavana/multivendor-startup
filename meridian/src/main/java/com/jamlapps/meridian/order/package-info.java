/**
 * Orders.
 *
 * <p>Implements BR-ORD of the business requirements document. Delivered in phase 1 and 4.
 * Allowed dependencies: catalog, vendor, pricing, payment::events.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Orders",
        allowedDependencies = { "catalog", "vendor", "pricing", "payment::events" })
package com.jamlapps.meridian.order;
