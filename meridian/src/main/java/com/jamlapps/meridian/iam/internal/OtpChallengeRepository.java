package com.jamlapps.meridian.iam.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OtpChallengeRepository extends JpaRepository<OtpChallengeEntity, UUID> {

    /**
     * The newest unconsumed challenge for a purpose. Requesting a second code does not invalidate
     * the first in the database, so verification must pick the latest rather than assume one exists
     * — a user who taps "resend" then types the code from the first SMS is a normal case, not an
     * attack, and is handled by the caller falling back through recent challenges.
     */
    Optional<OtpChallengeEntity> findFirstByAccountIdAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(
            UUID accountId, String purpose);
}
