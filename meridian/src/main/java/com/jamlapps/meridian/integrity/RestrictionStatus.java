package com.jamlapps.meridian.integrity;

/** BR-SEC-07: a restriction is always one of exactly these two states — there is no third, terminal one. */
public enum RestrictionStatus {

    /** Currently in effect. */
    ACTIVE,

    /** BR-SEC-07/AC-SEC-02: lifted in full by an admin. Never automatic — see {@code integrity.package-info}. */
    LIFTED
}
