/**
 * Pricing and Configuration.
 *
 * <p>Implements COM-01 to COM-05, DAT-07, DAT-08 of the business requirements document. Delivered in phase 0 and 3.
 * Depends on no other module.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Pricing and Configuration",
        allowedDependencies = {})
package com.jamlapps.meridian.pricing;
