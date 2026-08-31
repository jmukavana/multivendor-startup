package com.jamlapps.meridian.iam;

public enum AccountStatus {

    /** Registered but no identifier verified yet. Cannot check out (BR-IAM-02). */
    PENDING_VERIFICATION,

    ACTIVE,

    /** Suspended by an admin or an automated control. Sessions are revoked on entry (BR-SEC-07). */
    SUSPENDED,

    CLOSED
}
