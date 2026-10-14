package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.Vendor;
import com.jamlapps.meridian.vendor.Vendors;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class VendorsAdapter implements Vendors {

    private final VendorRepository vendors;
    private final PayoutDetailRepository payoutDetails;
    private final Clock clock;

    VendorsAdapter(VendorRepository vendors, PayoutDetailRepository payoutDetails, Clock clock) {
        this.vendors = vendors;
        this.payoutDetails = payoutDetails;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vendor> findById(UUID vendorId) {
        return vendors.findById(vendorId).map(VendorEntity::toVendor);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Vendor> findByOwnerAccountId(UUID ownerAccountId) {
        return vendors.findByOwnerAccountId(ownerAccountId).map(VendorEntity::toVendor);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean exists(UUID vendorId) {
        return vendors.existsById(vendorId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> payoutCoolingOffUntil(UUID vendorId) {
        return payoutDetails.findByVendorId(vendorId).map(PayoutDetailEntity::coolingOffUntil);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> vendorsInPayoutCoolingOff() {
        return payoutDetails.findByCoolingOffUntilAfter(clock.instant()).stream().map(PayoutDetailEntity::vendorId).toList();
    }
}
