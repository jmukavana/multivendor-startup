package com.jamlapps.meridian.iam;

import java.time.Instant;
import java.util.UUID;

/** The token exists only here, in the response that issues it — never stored, never logged, matching every other session-issuing response in this codebase. */
public record IssuedImpersonationSession(String token, Instant expiresAt, UUID targetAccountId, UUID impersonatorAccountId) {}
