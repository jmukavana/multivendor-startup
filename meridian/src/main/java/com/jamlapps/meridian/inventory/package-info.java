/**
 * Inventory.
 *
 * <p>Implements BR-INV of the business requirements document. Delivered in phase 2.
 * Allowed dependencies: catalog.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Inventory",
        allowedDependencies = { "catalog" })
package com.jamlapps.meridian.inventory;
