package com.jamlapps.meridian.notification.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-NTF-04. Absence of a row means "not opted out" — see {@code V23__notification.sql}. */
@Entity
@Table(name = "notification_preference")
class NotificationPreferenceEntity {

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "marketing_opt_out", nullable = false)
    private boolean marketingOptOut;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreferenceEntity() {
        // for JPA
    }

    NotificationPreferenceEntity(UUID accountId, boolean marketingOptOut, Instant updatedAt) {
        this.accountId = accountId;
        this.marketingOptOut = marketingOptOut;
        this.updatedAt = updatedAt;
    }

    UUID accountId() {
        return accountId;
    }

    boolean marketingOptOut() {
        return marketingOptOut;
    }

    void update(boolean marketingOptOut, Instant now) {
        this.marketingOptOut = marketingOptOut;
        this.updatedAt = now;
    }
}
