package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.RestrictionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RestrictionRepository extends JpaRepository<RestrictionEntity, UUID> {

    Optional<RestrictionEntity> findByScopeAndKeyAndStatus(String scope, String key, RestrictionStatus status);

    List<RestrictionEntity> findByStatusOrderByCreatedAtDesc(RestrictionStatus status);
}
