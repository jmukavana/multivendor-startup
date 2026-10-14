package com.jamlapps.meridian.inventory.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OversellAttemptRepository extends JpaRepository<OversellAttemptEntity, UUID> {

    List<OversellAttemptEntity> findAllByOrderByOccurredAtDesc();
}
