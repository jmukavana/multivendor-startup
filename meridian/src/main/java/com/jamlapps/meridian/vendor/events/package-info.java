/**
 * Events published by the vendor module.
 *
 * <p>Exposed as a named interface so other modules may depend on the events without gaining access
 * to the rest of {@code vendor}. Consumers listen with {@code @ApplicationModuleListener}.
 */
@org.springframework.modulith.NamedInterface("events")
package com.jamlapps.meridian.vendor.events;
