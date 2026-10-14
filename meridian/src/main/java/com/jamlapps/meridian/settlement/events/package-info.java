/**
 * Events published by the settlement module.
 *
 * <p>Exposed as a named interface so other modules may depend on the events without gaining access
 * to the rest of {@code settlement}. Consumers listen with {@code @ApplicationModuleListener}.
 *
 * <p>{@code notification}'s package-info already declares {@code settlement::events} as an allowed
 * dependency, in anticipation of this module — matching how {@code vendor::events} was declared
 * there before any listener for it existed. No listener consumes {@link VendorDisbursed} yet;
 * wiring "you have been paid" notifications is separate future work, not a precondition for this
 * event existing.
 */
@org.springframework.modulith.NamedInterface("events")
package com.jamlapps.meridian.settlement.events;
