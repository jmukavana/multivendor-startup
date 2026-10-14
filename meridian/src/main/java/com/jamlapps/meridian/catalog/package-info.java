/**
 * Catalog.
 *
 * <p>Implements BR-CAT of the business requirements document. Delivered in phase 1 and 2.
 * Allowed dependencies: vendor, vendor::events.
 *
 * <p>{@code vendor::events} was added in Phase 2 so this module can listen for
 * {@code VendorSuspended} and delist that vendor's products (BR-VND-10) — see
 * {@code VendorSuspensionListener} — without querying vendor status per read. See ADR-004 and the
 * Phase 1 note in ADR-002 for the same pattern used by {@code notification}.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Catalog",
        allowedDependencies = {"vendor", "vendor::events", "web", "shared"})
package com.jamlapps.meridian.catalog;
