/**
 * Tax Invoicing.
 *
 * <p>Implements REG-04 to REG-08 of the business requirements document. Delivered in phase 3.
 * Allowed dependencies: order::events.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Tax Invoicing",
        allowedDependencies = { "order::events" })
package com.jamlapps.meridian.tax;
