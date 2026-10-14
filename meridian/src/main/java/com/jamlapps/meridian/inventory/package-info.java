/**
 * Inventory.
 *
 * <p>Implements BR-INV of the business requirements document. Delivered in phase 2.
 * Allowed dependencies: catalog, vendor, order::events.
 *
 * <p>{@code vendor} was added so stock-setting can authorise "does this caller own the vendor that
 * owns this product" — the same ownership resolution {@code catalog} already does for product
 * management, needed here because BR-INV-01's stock level is this module's own data, not
 * catalog's. {@code order::events} lets this module consume {@code OrderPaymentConfirmed} to drive
 * BR-INV-05's reservation-to-deduction conversion, per ADR-004 — {@code order} does not call into
 * {@code inventory} directly, and never gained a dependency on it.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Inventory",
        allowedDependencies = { "catalog", "vendor", "order::events" })
package com.jamlapps.meridian.inventory;
