package com.jamlapps.meridian.vendor;

/**
 * BR-VND-05. Matches {@code meridian-frontend/src/features/vendor-onboarding/types.ts}'s
 * {@code VendorApplicationStatus} exactly, including {@code SUSPENDED} — which is not a state this
 * enum's owning entity actually stores; see {@code VendorApplicationEntity}'s javadoc for why.
 */
public enum VendorApplicationStatus {
    DRAFT,
    SUBMITTED,
    UNDER_REVIEW,
    INFORMATION_REQUESTED,
    APPROVED,
    REJECTED,
    SUSPENDED
}
