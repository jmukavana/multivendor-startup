package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.Restriction;
import com.jamlapps.meridian.integrity.RestrictionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-SEC-07/08. */
@Entity
@Table(name = "integrity_restriction")
class RestrictionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String scope;

    @Column(name = "rate_limit_key", nullable = false)
    private String key;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RestrictionStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "lifted_at")
    private @Nullable Instant liftedAt;

    @Column(name = "lifted_by")
    private @Nullable UUID liftedBy;

    @Column(name = "lift_reason", columnDefinition = "text")
    private @Nullable String liftReason;

    protected RestrictionEntity() {
        // for JPA
    }

    RestrictionEntity(UUID id, String scope, String key, String reason, Instant createdAt) {
        this.id = id;
        this.scope = scope;
        this.key = key;
        this.reason = reason;
        this.status = RestrictionStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    String scope() {
        return scope;
    }

    String key() {
        return key;
    }

    RestrictionStatus status() {
        return status;
    }

    /** BR-SEC-07/AC-SEC-02: full reversal, always by an admin, never automatic. */
    void lift(UUID liftedBy, String liftReason, Instant now) {
        this.status = RestrictionStatus.LIFTED;
        this.liftedBy = liftedBy;
        this.liftReason = liftReason;
        this.liftedAt = now;
    }

    Restriction toRestriction() {
        return new Restriction(id, scope, key, reason, status, createdAt, liftedAt, liftedBy, liftReason);
    }
}
