/**
 * Returns, Refunds and Disputes.
 *
 * <p>Implements BR-RTN of the business requirements document. Delivered in phase 5.
 * Allowed dependencies: order, payment.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Returns, Refunds and Disputes",
        allowedDependencies = { "order", "payment" })
package com.jamlapps.meridian.returns;
