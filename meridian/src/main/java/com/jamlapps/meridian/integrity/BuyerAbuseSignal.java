package com.jamlapps.meridian.integrity;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BR-SEC-04: one buyer's cancellation, return and dispute rates against the platform median, as
 * {@code AbuseDetectionService} computes them. Present for every buyer with at least one
 * sub-order — {@code flagged} is what a caller actually acts on; the raw rates are here so an
 * admin reviewing a flag can see why, not just that.
 */
public record BuyerAbuseSignal(
        UUID buyerAccountId,
        BigDecimal cancellationRate,
        BigDecimal returnRate,
        BigDecimal disputeRate,
        boolean flagged) {}
