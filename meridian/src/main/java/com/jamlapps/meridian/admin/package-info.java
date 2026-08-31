/**
 * Administration and Back-Office.
 *
 * <p>Implements BR-ADM of the business requirements document. Delivered in phase 2 onward.
 * Allowed dependencies: vendor, catalog, order, payment, ledger, settlement, returns, pricing, integrity.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Administration and Back-Office",
        allowedDependencies = { "vendor", "catalog", "order", "payment", "ledger", "settlement", "returns", "pricing", "integrity" })
package com.jamlapps.meridian.admin;
