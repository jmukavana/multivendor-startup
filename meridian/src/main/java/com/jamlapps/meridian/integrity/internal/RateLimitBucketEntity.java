package com.jamlapps.meridian.integrity.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * BR-SEC-01/02: one row per (scope, key, window). {@link RateLimitBucketRepository#upsertIncrement}
 * is the only writer — this entity exists to be read back after that native upsert, never saved
 * directly, so it carries no mutating methods the way every other entity in this codebase does.
 */
@Entity
@Table(name = "rate_limit_bucket")
class RateLimitBucketEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String scope;

    @Column(name = "rate_limit_key", nullable = false)
    private String key;

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private int count;

    protected RateLimitBucketEntity() {
        // for JPA
    }

    int count() {
        return count;
    }
}
