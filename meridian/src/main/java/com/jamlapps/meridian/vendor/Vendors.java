package com.jamlapps.meridian.vendor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** How other modules ask about vendors. Read-only, matching {@code iam.Accounts}' reasoning. */
public interface Vendors {

    Optional<Vendor> findById(UUID vendorId);

    Optional<Vendor> findByOwnerAccountId(UUID ownerAccountId);

    boolean exists(UUID vendorId);

    /**
     * BR-VND-09, BR-STL-08: the instant before which a settlement run must exclude this vendor,
     * because its payout details changed too recently. Empty when the vendor has no payout details
     * on file at all, which is not the same as "not in cooling off" — Phase 6 must treat "no payout
     * details" as its own exclusion reason rather than reading this as a green light.
     */
    Optional<Instant> payoutCoolingOffUntil(UUID vendorId);

    /**
     * BR-SEC-05: "payout detail changes within a configurable period before a settlement run" —
     * every vendor {@link #payoutCoolingOffUntil} would currently exclude, in one call rather than
     * one per vendor. The same underlying state BR-VND-09/BR-STL-08 already enforce; this exposes
     * it as a fraud signal, not a new mechanism.
     */
    List<UUID> vendorsInPayoutCoolingOff();
}
