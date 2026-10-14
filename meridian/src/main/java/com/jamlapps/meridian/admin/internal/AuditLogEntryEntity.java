package com.jamlapps.meridian.admin.internal;

import com.jamlapps.meridian.admin.AuditLogEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-ADM-07. Immutable: no mutator method exists on this class, and none should ever be added. */
@Entity
@Table(name = "audit_log_entry")
class AuditLogEntryEntity {

    @Id
    private UUID id;

    @Column(name = "actor_account_id")
    private @Nullable UUID actorAccountId;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(columnDefinition = "text")
    private @Nullable String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLogEntryEntity() {
        // for JPA
    }

    AuditLogEntryEntity(
            UUID id,
            @Nullable UUID actorAccountId,
            String action,
            String entityType,
            String entityId,
            @Nullable String detail,
            Instant occurredAt) {
        this.id = id;
        this.actorAccountId = actorAccountId;
        this.action = action;
        this.entityType = entityType;
        this.entityId = entityId;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    AuditLogEntry toAuditLogEntry() {
        return new AuditLogEntry(id, actorAccountId, action, entityType, entityId, detail, occurredAt);
    }
}
