package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.web.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves how a caller relates to a specific vendor — owner, staff, or unrelated — and lets an
 * owner add staff. See V7's comment: this is a per-vendor relationship, not the global
 * {@code iam.Role.VENDOR_STAFF} constant.
 */
@Service
class VendorMembershipService {

    private final VendorRepository vendors;
    private final VendorStaffRepository staff;
    private final Clock clock;

    VendorMembershipService(VendorRepository vendors, VendorStaffRepository staff, Clock clock) {
        this.vendors = vendors;
        this.staff = staff;
        this.clock = clock;
    }

    enum Relationship {
        OWNER,
        STAFF,
        UNRELATED
    }

    @Transactional(readOnly = true)
    Relationship relationshipOf(UUID vendorId, UUID accountId) {
        if (!vendors.existsById(vendorId)) {
            throw ApiException.notFound("Vendor", vendorId);
        }
        if (vendors.existsByIdAndOwnerAccountId(vendorId, accountId)) {
            return Relationship.OWNER;
        }
        if (staff.existsByVendorIdAndAccountId(vendorId, accountId)) {
            return Relationship.STAFF;
        }
        return Relationship.UNRELATED;
    }

    /** Owner-only, enforced by the caller resolving {@link #relationshipOf} first. */
    @Transactional
    void addStaff(UUID vendorId, UUID staffAccountId) {
        if (!vendors.existsById(vendorId)) {
            throw ApiException.notFound("Vendor", vendorId);
        }
        Instant now = clock.instant();
        staff.save(new VendorStaffEntity(vendorId, staffAccountId, now));
    }
}
