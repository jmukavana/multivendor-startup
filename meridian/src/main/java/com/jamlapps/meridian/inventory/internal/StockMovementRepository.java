package com.jamlapps.meridian.inventory.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockMovementRepository extends JpaRepository<StockMovementEntity, UUID> {

    List<StockMovementEntity> findByProductIdOrderByOccurredAtDesc(UUID productId);
}
