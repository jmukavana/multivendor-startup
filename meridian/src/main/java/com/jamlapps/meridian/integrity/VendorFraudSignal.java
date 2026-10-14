package com.jamlapps.meridian.integrity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BR-SEC-05: one vendor's dispatch-failure rate against the platform median, plus whether its
 * payout details changed recently enough to still be in BR-VND-09's cooling-off window — two
 * independent signals surfaced together, not combined into one score. {@code
 * payoutRecentlyChanged} alone marks {@code flagged}, regardless of the dispatch-failure
 * comparison: a vendor whose payout details changed just before a settlement run is worth an
 * admin's attention on that basis alone, real or not by the other measure.
 */
public record VendorFraudSignal(UUID vendorId, BigDecimal dispatchFailureRate, boolean payoutRecentlyChanged, boolean flagged) {}
