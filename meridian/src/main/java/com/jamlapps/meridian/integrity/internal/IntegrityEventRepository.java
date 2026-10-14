package com.jamlapps.meridian.integrity.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface IntegrityEventRepository extends JpaRepository<IntegrityEventEntity, UUID> {

    long countByScopeAndKeyAndTypeAndOccurredAtAfter(String scope, String key, String type, Instant after);
}
