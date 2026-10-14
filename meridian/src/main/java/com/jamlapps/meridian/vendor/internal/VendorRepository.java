package com.jamlapps.meridian.vendor.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VendorRepository extends JpaRepository<VendorEntity, UUID> {

    Optional<VendorEntity> findByOwnerAccountId(UUID ownerAccountId);

    /**
     * Owner-check as a WHERE clause rather than fetching the vendor to compare one field — used by
     * {@code VendorMembershipService.relationshipOf}. Same reasoning as
     * {@code catalog.ProductRepository.findByIdAndVendorId}.
     */
    boolean existsByIdAndOwnerAccountId(UUID id, UUID ownerAccountId);
}
