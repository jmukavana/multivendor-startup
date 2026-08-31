package com.jamlapps.meridian.iam.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A pending one-time code.
 *
 * <p>Six digits is a million possibilities, which an attacker can exhaust in seconds given enough
 * requests. Three things make it safe and all three are required: a slow hash, a short expiry, and
 * the attempt limit enforced here.
 */
@Entity
@Table(name = "otp_challenge")
class OtpChallengeEntity {

    /** Beyond this many wrong guesses the challenge is dead and a new code must be requested. */
    static final int MAX_ATTEMPTS = 5;

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(nullable = false)
    private String purpose;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private @Nullable Instant consumedAt;

    @Column(nullable = false)
    private int attempts;

    protected OtpChallengeEntity() {
        // for JPA
    }

    OtpChallengeEntity(
            UUID id, UUID accountId, String purpose, String codeHash, Instant issuedAt, Instant expiresAt) {
        this.id = id;
        this.accountId = accountId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.attempts = 0;
    }

    boolean isRedeemable(Instant now) {
        return consumedAt == null && attempts < MAX_ATTEMPTS && expiresAt.isAfter(now);
    }

    void recordFailedAttempt() {
        attempts++;
    }

    void consume(Instant now) {
        this.consumedAt = now;
    }

    String codeHash() {
        return codeHash;
    }

    UUID id() {
        return id;
    }

    UUID accountId() {
        return accountId;
    }
}
