package com.jamlapps.meridian.iam.events;

import java.time.Instant;
import java.util.UUID;

/**
 * A one-time code has been issued and needs delivering to the account holder.
 *
 * <p>{@code iam} does not send messages. It publishes this, and {@code notification} delivers it
 * over SMS or email — which is why {@code iam} depends on no other module despite needing an SMS
 * gateway to function.
 *
 * <p><strong>The code is not in this event, and must never be.</strong> It travels to the
 * notification module through {@code deliveryReference}, which resolves against the challenge
 * record. An event is persisted in the publication registry, replayed on failure, and serialised
 * into a table readable by anyone with database access; a plaintext one-time code in that table is
 * a credential at rest.
 *
 * @param destination the phone number or email address to deliver to
 * @param deliveryReference identifies the challenge, so the sender can resolve the code without it
 *     ever being written to the event log
 */
public record OneTimeCodeIssued(
        UUID accountId, UUID deliveryReference, String destination, String purpose, Instant expiresAt) {}
