package com.jamlapps.meridian.integrity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * BR-SEC-08: "log every automated restriction and every manual override ... in the immutable audit
 * log under BR-ADM-07." No such unified admin-level log exists yet — {@code admin} is not built —
 * so this table is this module's own, closest-available equivalent; see {@code package-info}'s
 * note on the gap. Nothing in this codebase ever issues an UPDATE or DELETE against it.
 */
@Entity
@Table(name = "integrity_event")
class IntegrityEventEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String scope;

    @Column(name = "rate_limit_key", nullable = false)
    private String key;

    @Column(columnDefinition = "text")
    private @Nullable String detail;

    @Column(name = "actor_account_id")
    private @Nullable UUID actorAccountId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected IntegrityEventEntity() {
        // for JPA
    }

    IntegrityEventEntity(
            UUID id, String type, String scope, String key, @Nullable String detail, @Nullable UUID actorAccountId, Instant occurredAt) {
        this.id = id;
        this.type = type;
        this.scope = scope;
        this.key = key;
        this.detail = detail;
        this.actorAccountId = actorAccountId;
        this.occurredAt = occurredAt;
    }
}
