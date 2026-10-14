package com.jamlapps.meridian.vendor.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface VendorStaffRepository extends JpaRepository<VendorStaffEntity, VendorStaffEntity.Key> {

    boolean existsByVendorIdAndAccountId(UUID vendorId, UUID accountId);
}
