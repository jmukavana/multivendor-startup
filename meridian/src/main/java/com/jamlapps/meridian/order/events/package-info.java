/**
 * Events published by the orders module.
 *
 * <p>Exposed as a named interface so that other modules may depend on the events without gaining
 * access to the rest of the module. Consumers listen with {@code @ApplicationModuleListener};
 * they must not call back into this module synchronously.
 */
@org.springframework.modulith.NamedInterface("events")
package com.jamlapps.meridian.order.events;
