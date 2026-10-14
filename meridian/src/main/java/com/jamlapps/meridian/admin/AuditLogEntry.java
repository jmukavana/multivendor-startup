package com.jamlapps.meridian.admin;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-ADM-07: one immutable audit row, as the admin audit trail view sees it. */
public record AuditLogEntry(
        UUID id, @Nullable UUID actorAccountId, String action, String entityType, String entityId, @Nullable String detail, Instant occurredAt) {}
