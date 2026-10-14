package com.jamlapps.meridian.settlement.internal;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SettlementHoldRepository extends JpaRepository<SettlementHoldEntity, UUID> {

    /** BR-STL-09: at most one active (unreleased) hold per vendor, enforced in {@code SettlementService}, not here. */
    Optional<SettlementHoldEntity> findByVendorIdAndReleasedAtIsNull(UUID vendorId);
}
