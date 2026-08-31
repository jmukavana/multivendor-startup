/**
 * Notifications.
 *
 * <p>Implements BR-NTF of the business requirements document. Delivered in phase 2 onward.
 * Allowed dependencies: iam::events, order::events, payment::events, settlement::events, returns::events, vendor::events.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = { "iam::events", "order::events", "payment::events", "settlement::events", "returns::events", "vendor::events" })
package com.jamlapps.meridian.notification;
