package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.DisputeStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DisputeRepository extends JpaRepository<DisputeEntity, UUID> {

    Optional<DisputeEntity> findByReturnId(UUID returnId);

    /** BR-RTN-06: the admin queue. */
    List<DisputeEntity> findByStatusOrderByCreatedAt(DisputeStatus status);
}
