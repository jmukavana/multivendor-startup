package com.jamlapps.meridian.iam.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByPhoneNumber(String phoneNumber);

    /** Email is matched case-insensitively, matching the functional unique index in V2. */
    @Query("select a from AccountEntity a where lower(a.email) = lower(:email)")
    Optional<AccountEntity> findByEmailIgnoringCase(String email);
}
