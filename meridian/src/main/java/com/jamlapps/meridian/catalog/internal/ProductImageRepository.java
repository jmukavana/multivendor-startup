package com.jamlapps.meridian.catalog.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {

    List<ProductImageEntity> findByProductId(UUID productId);

    long countByProductId(UUID productId);
}
