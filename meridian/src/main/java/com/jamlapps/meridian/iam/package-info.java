/**
 * Identity and Access.
 *
 * <p>Implements BR-IAM of the business requirements document. Delivered in phase 1.
 * Depends on no other module.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Identity and Access",
        allowedDependencies = {})
package com.jamlapps.meridian.iam;
