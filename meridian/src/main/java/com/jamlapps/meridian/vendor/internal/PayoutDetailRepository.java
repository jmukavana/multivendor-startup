package com.jamlapps.meridian.vendor.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PayoutDetailRepository extends JpaRepository<PayoutDetailEntity, UUID> {

    Optional<PayoutDetailEntity> findByVendorId(UUID vendorId);

    /** BR-SEC-05: {@code Vendors.vendorsInPayoutCoolingOff}'s query. */
    List<PayoutDetailEntity> findByCoolingOffUntilAfter(Instant now);
}
