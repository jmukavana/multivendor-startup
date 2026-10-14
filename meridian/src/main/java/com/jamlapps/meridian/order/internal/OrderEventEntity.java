package com.jamlapps.meridian.order.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-ORD-10: immutable audit trail of every order state change, with actor and timestamp. */
@Entity
@Table(name = "order_event")
class OrderEventEntity {

    @Id
    private UUID id;

    @Column(name = "parent_order_id", nullable = false)
    private UUID parentOrderId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "previous_status")
    private @Nullable String previousStatus;

    @Column(name = "new_status")
    private @Nullable String newStatus;

    @Column(name = "actor_account_id")
    private @Nullable UUID actorAccountId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OrderEventEntity() {
        // for JPA
    }

    OrderEventEntity(
            UUID id,
            UUID parentOrderId,
            String eventType,
            @Nullable String previousStatus,
            @Nullable String newStatus,
            @Nullable UUID actorAccountId,
            Instant occurredAt) {
        this.id = id;
        this.parentOrderId = parentOrderId;
        this.eventType = eventType;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.actorAccountId = actorAccountId;
        this.occurredAt = occurredAt;
    }
}
