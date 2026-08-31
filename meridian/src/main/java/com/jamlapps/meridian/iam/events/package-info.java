/**
 * Events published by the identity and access module.
 *
 * <p>Exposed as a named interface so other modules may depend on the events without gaining access
 * to the rest of {@code iam}. Consumers listen with {@code @ApplicationModuleListener}.
 */
@org.springframework.modulith.NamedInterface("events")
package com.jamlapps.meridian.iam.events;
