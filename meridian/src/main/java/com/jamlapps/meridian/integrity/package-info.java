/**
 * Platform Integrity.
 *
 * <p>Implements BR-SEC of the business requirements document. Delivered in phase 9, with rate limiting in 1.
 * Allowed dependencies: iam.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Platform Integrity",
        allowedDependencies = { "iam" })
package com.jamlapps.meridian.integrity;
