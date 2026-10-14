package com.jamlapps.meridian.iam.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** A session, identified by the hash of a token the client holds. See ADR-008. */
@Entity
@Table(name = "session_token")
class SessionTokenEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private @Nullable Instant revokedAt;

    @Column(name = "last_used_at")
    private @Nullable Instant lastUsedAt;

    @Column(name = "source_address")
    private @Nullable String sourceAddress;

    @Column(name = "user_agent")
    private @Nullable String userAgent;

    /** BR-ADM-08: null for every ordinary session — only set on one {@code AuthenticationService.issueImpersonationSession} creates. */
    @Column(name = "impersonator_account_id")
    private @Nullable UUID impersonatorAccountId;

    protected SessionTokenEntity() {
        // for JPA
    }

    SessionTokenEntity(
            UUID id,
            UUID accountId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            @Nullable String sourceAddress,
            @Nullable String userAgent) {
        this(id, accountId, tokenHash, issuedAt, expiresAt, sourceAddress, userAgent, null);
    }

    SessionTokenEntity(
            UUID id,
            UUID accountId,
            String tokenHash,
            Instant issuedAt,
            Instant expiresAt,
            @Nullable String sourceAddress,
            @Nullable String userAgent,
            @Nullable UUID impersonatorAccountId) {
        this.id = id;
        this.accountId = accountId;
        this.tokenHash = tokenHash;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.sourceAddress = sourceAddress;
        this.userAgent = userAgent;
        this.impersonatorAccountId = impersonatorAccountId;
    }

    boolean isLive(Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    void revoke(Instant now) {
        if (revokedAt == null) {
            this.revokedAt = now;
        }
    }

    void touch(Instant now) {
        this.lastUsedAt = now;
    }

    UUID accountId() {
        return accountId;
    }

    UUID id() {
        return id;
    }

    Instant issuedAt() {
        return issuedAt;
    }

    Instant expiresAt() {
        return expiresAt;
    }

    @Nullable Instant revokedAt() {
        return revokedAt;
    }

    @Nullable UUID impersonatorAccountId() {
        return impersonatorAccountId;
    }
}
