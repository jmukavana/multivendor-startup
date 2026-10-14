/**
 * Events published by the fulfilment module.
 *
 * <p>Exposed as a named interface so other modules may depend on the events without gaining access
 * to the rest of {@code fulfilment}. Consumers listen with {@code @ApplicationModuleListener}.
 */
@org.springframework.modulith.NamedInterface("events")
package com.jamlapps.meridian.fulfilment.events;
