package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.Dispute;
import com.jamlapps.meridian.returns.DisputeFavor;
import com.jamlapps.meridian.returns.DisputeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-RTN-03/06/07. */
@Entity
@Table(name = "dispute")
class DisputeEntity {

    @Id
    private UUID id;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisputeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_favor")
    private @Nullable DisputeFavor resolutionFavor;

    @Column(name = "resolution_reason")
    private @Nullable String resolutionReason;

    @Column(name = "resolved_by_account_id")
    private @Nullable UUID resolvedByAccountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    protected DisputeEntity() {
        // for JPA
    }

    DisputeEntity(UUID id, UUID returnId, Instant createdAt) {
        this.id = id;
        this.returnId = returnId;
        this.status = DisputeStatus.OPEN;
        this.createdAt = createdAt;
    }

    /** BR-RTN-07, AC-RTN-04: {@code reason} is validated non-blank by the controller before this is called. */
    void resolve(DisputeFavor favor, String reason, UUID resolvedByAccountId, Instant now) {
        this.status = DisputeStatus.RESOLVED;
        this.resolutionFavor = favor;
        this.resolutionReason = reason;
        this.resolvedByAccountId = resolvedByAccountId;
        this.resolvedAt = now;
    }

    UUID id() {
        return id;
    }

    UUID returnId() {
        return returnId;
    }

    DisputeStatus status() {
        return status;
    }

    Dispute toDispute() {
        return new Dispute(id, returnId, status, resolutionFavor, resolutionReason, resolvedByAccountId, createdAt, resolvedAt);
    }
}
