/**
 * Catalog.
 *
 * <p>Implements BR-CAT of the business requirements document. Delivered in phase 1 and 2.
 * Allowed dependencies: vendor.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Catalog",
        allowedDependencies = { "vendor" })
package com.jamlapps.meridian.catalog;
