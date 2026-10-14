package com.jamlapps.meridian.returns;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-RTN-06/07: created on vendor rejection or a response timeout; resolved by an admin with a mandatory reason (AC-RTN-04). */
public record Dispute(
        UUID id,
        UUID returnId,
        DisputeStatus status,
        @Nullable DisputeFavor resolutionFavor,
        @Nullable String resolutionReason,
        @Nullable UUID resolvedByAccountId,
        Instant createdAt,
        @Nullable Instant resolvedAt) {}
