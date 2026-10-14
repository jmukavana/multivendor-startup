package com.jamlapps.meridian.vendor;

import java.time.Instant;
import java.util.UUID;

/**
 * A vendor, as other modules see it.
 *
 * <p>Phase 1's self-service creation is gone: a {@code Vendor} row now exists only because a
 * {@link VendorApplication} was approved (BR-VND-04). {@code status} is what makes BR-VND-07
 * ("an unapproved vendor may not publish or sell") and BR-VND-10 (suspension) enforceable —
 * {@code catalog} checks it before listing a product, and before showing one already listed.
 */
public record Vendor(UUID id, UUID ownerAccountId, String businessName, VendorStatus status, Instant createdAt) {

    public boolean isActive() {
        return status == VendorStatus.ACTIVE;
    }
}
