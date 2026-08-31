/**
 * Shared kernel.
 *
 * <p>Value types with no business behaviour of their own, used across modules. Declared
 * {@link org.springframework.modulith.ApplicationModule.Type#OPEN} so its internals are visible
 * everywhere, and registered as a shared module on the application class so no module needs to
 * declare a dependency on it.
 *
 * <p>Nothing that carries business rules belongs here. A type that acquires behaviour owned by one
 * module should move into that module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Shared Kernel",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.jamlapps.meridian.shared;
