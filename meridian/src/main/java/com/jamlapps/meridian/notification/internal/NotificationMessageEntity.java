package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.notification.NotificationChannel;
import com.jamlapps.meridian.notification.NotificationMessage;
import com.jamlapps.meridian.notification.NotificationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-NTF-01 to 05. Mirrors {@code tax.internal.TaxInvoiceEntity}'s retry/dead-letter shape exactly. */
@Entity
@Table(name = "notification_message")
class NotificationMessageEntity {

    static final int MAX_ATTEMPTS = 3;

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String recipient;

    @Column
    private @Nullable String subject;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private @Nullable Instant sentAt;

    protected NotificationMessageEntity() {
        // for JPA
    }

    NotificationMessageEntity(
            UUID id, NotificationChannel channel, String recipient, @Nullable String subject, String body, Instant createdAt) {
        this.id = id;
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.PENDING;
        this.createdAt = createdAt;
    }

    void recordSuccess(Instant now) {
        this.status = NotificationStatus.SENT;
        this.sentAt = now;
    }

    /** BR-NTF-05: retried up to {@link #MAX_ATTEMPTS} before the dead letter. */
    void recordFailure() {
        this.attemptCount++;
        this.status = attemptCount >= MAX_ATTEMPTS ? NotificationStatus.DEAD_LETTERED : NotificationStatus.FAILED;
    }

    UUID id() {
        return id;
    }

    NotificationChannel channel() {
        return channel;
    }

    String recipient() {
        return recipient;
    }

    String body() {
        return body;
    }

    NotificationStatus status() {
        return status;
    }

    NotificationMessage toNotificationMessage() {
        return new NotificationMessage(id, channel, recipient, subject, body, status, attemptCount, createdAt, sentAt);
    }
}
