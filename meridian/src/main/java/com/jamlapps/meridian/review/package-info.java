/**
 * Reviews and Ratings.
 *
 * <p>Implements BR-REV of the business requirements document. Delivered in phase 8.
 * Allowed dependencies: order, catalog.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Reviews and Ratings",
        allowedDependencies = { "order", "catalog" })
package com.jamlapps.meridian.review;
