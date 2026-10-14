package com.jamlapps.meridian.integrity;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * BR-SEC-07/08: an automated restriction, as the admin exception queue sees it — the triggering
 * signal ({@code scope}/{@code key}/{@code reason}), and, once lifted, who lifted it and why.
 */
public record Restriction(
        UUID id,
        String scope,
        String key,
        String reason,
        RestrictionStatus status,
        Instant createdAt,
        @Nullable Instant liftedAt,
        @Nullable UUID liftedBy,
        @Nullable String liftReason) {}
