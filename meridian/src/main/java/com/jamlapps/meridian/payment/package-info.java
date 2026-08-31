/**
 * Payments.
 *
 * <p>Implements BR-PAY of the business requirements document. Delivered in phase 3.
 * Depends on no other module.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Payments",
        allowedDependencies = {})
package com.jamlapps.meridian.payment;
