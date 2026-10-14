package com.jamlapps.meridian.vendor.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-IAM-04, AC-VND-03: every attempt to view or change payout details, allowed or refused. */
@Entity
@Table(name = "payout_detail_access_event")
class PayoutDetailAccessEventEntity {

    enum Action {
        VIEW,
        CHANGE
    }

    enum Outcome {
        ALLOWED,
        REFUSED
    }

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "actor_account_id", nullable = false)
    private UUID actorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Outcome outcome;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected PayoutDetailAccessEventEntity() {
        // for JPA
    }

    PayoutDetailAccessEventEntity(
            UUID id, UUID vendorId, UUID actorAccountId, Action action, Outcome outcome, Instant occurredAt) {
        this.id = id;
        this.vendorId = vendorId;
        this.actorAccountId = actorAccountId;
        this.action = action;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
    }
}
