package com.jamlapps.meridian.iam;

/**
 * The actors in BRD Section 8 that an account can be, per BR-IAM-03.
 *
 * <p>Guest and System are absent deliberately. A guest holds no account — BR-IAM-08 permits
 * browsing without registration, which means there is nothing to assign a role to. System denotes
 * scheduled processes, which authenticate as themselves rather than as an account.
 */
public enum Role {
    BUYER,
    VENDOR_OWNER,

    /**
     * Delegated vendor user. BR-IAM-04 forbids access to payout banking details, which the vendor
     * module enforces; the distinction exists here so it can.
     */
    VENDOR_STAFF,

    PLATFORM_ADMIN,
    FINANCE_ADMIN,
    SUPER_ADMIN;

    /** The authority name Spring Security matches on, for {@code hasRole('FINANCE_ADMIN')}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
