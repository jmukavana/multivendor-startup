package com.jamlapps.meridian.settlement;

import java.util.UUID;

/**
 * BR-ADM-06, DAT-07, OPEN-04: the configured minimum interval, in days, between two disbursements
 * to the same vendor — versioned, never updated in place, the same shape as {@code
 * pricing.CommissionRate}/{@code pricing.HoldbackRate} for the same reason. Unlike those two,
 * this value is not resolved once per order and carried forward; {@code SettlementService}
 * resolves whichever configuration is current at the moment each settlement run is calculated
 * (see that class's javadoc for why that is the correct behaviour here, not a shortcut).
 */
public record SettlementCycle(UUID configurationVersionId, int cycleDays) {}
