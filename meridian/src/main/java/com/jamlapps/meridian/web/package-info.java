/**
 * The HTTP API contract: response envelope, error shape, and the plumbing that produces both.
 *
 * <p>Declared {@link org.springframework.modulith.ApplicationModule.Type#OPEN} and registered as a
 * shared module, so any module can return {@link com.jamlapps.meridian.web.ApiResponse} from a
 * controller without declaring a dependency. Every module needs it; making each one declare it
 * would add noise to eighteen files and enforce nothing.
 *
 * <p>The shape is not ours alone. {@code meridian-frontend/src/shared/api} already consumes this
 * envelope, these error codes and this problem shape. Changing anything here without changing that
 * breaks the frontend at runtime, not at compile time.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "API Contract",
        type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.jamlapps.meridian.web;
