package com.jamlapps.meridian.catalog.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductPriceChangeRepository extends JpaRepository<ProductPriceChangeEntity, UUID> {

    List<ProductPriceChangeEntity> findByProductIdOrderByChangedAtDesc(UUID productId);
}
