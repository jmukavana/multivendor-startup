package com.jamlapps.meridian.iam;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-ADM-08's "visible to the vendor": one past impersonation session on the caller's own account, without the token itself. */
public record ImpersonationRecord(UUID sessionId, UUID impersonatorAccountId, Instant issuedAt, Instant expiresAt, @Nullable Instant revokedAt) {}
