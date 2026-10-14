package com.jamlapps.meridian.settlement.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-STL-09: a manual hold on one vendor, with reason capture. */
@Entity
@Table(name = "settlement_hold")
class SettlementHoldEntity {

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(nullable = false)
    private String reason;

    @Column(name = "created_by_account_id", nullable = false)
    private UUID createdByAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "released_by_account_id")
    private @Nullable UUID releasedByAccountId;

    @Column(name = "released_at")
    private @Nullable Instant releasedAt;

    protected SettlementHoldEntity() {
        // for JPA
    }

    SettlementHoldEntity(UUID id, UUID vendorId, String reason, UUID createdByAccountId, Instant createdAt) {
        this.id = id;
        this.vendorId = vendorId;
        this.reason = reason;
        this.createdByAccountId = createdByAccountId;
        this.createdAt = createdAt;
    }

    void release(UUID actorAccountId, Instant now) {
        this.releasedByAccountId = actorAccountId;
        this.releasedAt = now;
    }

    UUID id() {
        return id;
    }

    UUID vendorId() {
        return vendorId;
    }

    String reason() {
        return reason;
    }
}
