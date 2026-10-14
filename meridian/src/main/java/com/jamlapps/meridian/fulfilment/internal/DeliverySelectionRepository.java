package com.jamlapps.meridian.fulfilment.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DeliverySelectionRepository extends JpaRepository<DeliverySelectionEntity, UUID> {

    Optional<DeliverySelectionEntity> findBySubOrderId(UUID subOrderId);
}
