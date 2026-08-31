package com.jamlapps.meridian.iam.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * BR-IAM-06: every authentication event, with timestamp, actor and source address.
 *
 * <p>Append-only, and not foreign-keyed to account, because the events most worth having are the
 * ones with no account behind them — a sign-in attempt against an identifier that does not exist.
 */
@Entity
@Table(name = "auth_event")
class AuthEventEntity {

    @Id
    private UUID id;

    @Column(name = "account_id")
    private @Nullable UUID accountId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "source_address")
    private @Nullable String sourceAddress;

    @Column(name = "user_agent")
    private @Nullable String userAgent;

    private @Nullable String detail;

    protected AuthEventEntity() {
        // for JPA
    }

    AuthEventEntity(
            UUID id,
            @Nullable UUID accountId,
            String eventType,
            Instant occurredAt,
            @Nullable String sourceAddress,
            @Nullable String userAgent,
            @Nullable String detail) {
        this.id = id;
        this.accountId = accountId;
        this.eventType = eventType;
        this.occurredAt = occurredAt;
        this.sourceAddress = sourceAddress;
        this.userAgent = userAgent;
        this.detail = detail;
    }
}
