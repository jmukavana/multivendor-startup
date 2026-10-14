/**
 * Events published by the returns module.
 *
 * <p>Exposed as a named interface so other modules may depend on the events without gaining access
 * to the rest of {@code returns}. Consumers listen with {@code @ApplicationModuleListener}.
 *
 * <p>{@code ledger} and {@code notification} both already declared {@code returns::events} as an
 * allowed dependency, in anticipation of this module — the same "declare ahead of the consumer"
 * pattern {@code vendor::events} and {@code settlement::events} were built under.
 */
@org.springframework.modulith.NamedInterface("events")
package com.jamlapps.meridian.returns.events;
