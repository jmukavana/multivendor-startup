package com.jamlapps.meridian.iam.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SessionTokenRepository extends JpaRepository<SessionTokenEntity, UUID> {

    Optional<SessionTokenEntity> findByTokenHash(String tokenHash);

    List<SessionTokenEntity> findByAccountIdAndRevokedAtIsNull(UUID accountId);

    /**
     * Revokes every live session for an account in one statement, for suspension (BR-VND-10,
     * BR-SEC-07) and for "sign out everywhere". Done as a bulk update because loading a
     * potentially large set of sessions to call a setter on each would be slower and no clearer.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SessionTokenEntity s set s.revokedAt = :now "
            + "where s.accountId = :accountId and s.revokedAt is null")
    int revokeAllForAccount(UUID accountId, Instant now);
}
