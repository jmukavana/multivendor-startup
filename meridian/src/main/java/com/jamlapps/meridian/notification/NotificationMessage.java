package com.jamlapps.meridian.notification;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A queued notification, as the admin exception queue and delivery-history lookup see it —
 * matching {@code tax.TaxInvoice}'s reasoning exactly: a public read model over an internal
 * entity, not something another module ever constructs or mutates.
 */
public record NotificationMessage(
        UUID id,
        NotificationChannel channel,
        String recipient,
        @Nullable String subject,
        String body,
        NotificationStatus status,
        int attemptCount,
        Instant createdAt,
        @Nullable Instant sentAt) {}
