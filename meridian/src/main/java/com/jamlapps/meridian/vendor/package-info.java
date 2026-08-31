/**
 * Vendor Onboarding and Management.
 *
 * <p>Implements BR-VND of the business requirements document. Delivered in phase 2.
 * Allowed dependencies: iam.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Vendor Onboarding and Management",
        allowedDependencies = { "iam" })
package com.jamlapps.meridian.vendor;
