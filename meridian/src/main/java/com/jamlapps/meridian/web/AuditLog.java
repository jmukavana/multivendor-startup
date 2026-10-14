package com.jamlapps.meridian.web;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * BR-ADM-07: "record every administrative action in an immutable audit log" — a slot in {@code
 * web}, the OPEN module every other module can already use, filled by {@code admin}'s real,
 * append-only implementation. The same "define the slot where it's used, fill it from where the
 * feature lives" pattern as {@code RateLimiter}: many modules write here (a vendor application
 * decision, a moderation removal, a configuration change), and none of them need a dependency on
 * {@code admin} to do it.
 *
 * <p>This is not a replacement for a module's own domain events or its own narrower audit trail
 * ({@code iam}'s {@code auth_event}, {@code tax}/{@code notification}/{@code integrity}'s own
 * transmission and breach logs) — those record what a module's own machinery did. This records
 * what an administrator decided, which is a different question with a different reader: an admin
 * reviewing "who did this and why", not a module reconstructing its own state machine.
 */
public interface AuditLog {

    /**
     * @param actorAccountId the admin who took the action; null only for a genuinely automated
     *     one (there are none yet — every current caller is a human admin's own decision)
     * @param action a short, stable verb this codebase controls, e.g. {@code "VENDOR_APPLICATION_APPROVED"}
     * @param entityType what kind of thing was acted on, e.g. {@code "VendorApplication"}
     * @param entityId the acted-on entity's id, as text — callers are not required to share one id type
     * @param detail free text a reviewer needs, such as a rejection or moderation reason; may be null
     */
    void record(@Nullable UUID actorAccountId, String action, String entityType, String entityId, @Nullable String detail);
}
