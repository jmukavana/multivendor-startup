package com.jamlapps.meridian.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * BR-ADM-06: how {@code admin} creates a new commission rate version. Kept separate from {@link
 * PricingConfiguration} rather than added to it — that interface is "how other modules ask about
 * pricing", read-only by design, matching {@code iam.Accounts}' reasoning; only {@code admin}
 * needs to write.
 */
public interface PricingAdministration {

    /**
     * COM-02, DAT-07: always a new, immutable row — {@code order} resolves the rate in force at
     * creation and stores that version id forever after (see {@code PricingConfiguration}'s
     * javadoc), so a configuration change is a new version becoming current, never an edit to an
     * old one. {@code effectiveFrom} in the past or now makes the new rate current immediately;
     * in the future, it becomes current only once {@code PricingConfiguration.currentCommissionRate}
     * is asked for after that instant — {@code commission_configuration_by_effective_idx}'s
     * "most recent row whose effective_from has passed" query already handles either case.
     */
    CommissionRate createCommissionConfiguration(BigDecimal rate, Instant effectiveFrom, UUID adminAccountId);

    /** BR-ADM-06: the same versioning discipline as {@link #createCommissionConfiguration}, for holdback. */
    HoldbackRate createHoldbackConfiguration(BigDecimal rate, Instant effectiveFrom, UUID adminAccountId);
}
