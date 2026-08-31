/**
 * Cart and Checkout.
 *
 * <p>Implements BR-CHK of the business requirements document. Delivered in phase 4.
 * Allowed dependencies: catalog, inventory, vendor, pricing, fulfilment, order, payment.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Cart and Checkout",
        allowedDependencies = { "catalog", "inventory", "vendor", "pricing", "fulfilment", "order", "payment" })
package com.jamlapps.meridian.checkout;
