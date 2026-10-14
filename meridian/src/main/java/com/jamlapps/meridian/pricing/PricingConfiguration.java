package com.jamlapps.meridian.pricing;

import java.util.UUID;

/**
 * COM-01 to 05, DAT-08; BR-ADM-06, DAT-07 for holdback. {@code order} calls
 * {@link #currentCommissionRate}/{@link #currentHoldbackRate} once each, at sub-order creation,
 * and stores the returned version ids — never "the current rate" — so that anything computed from
 * an order later (settlement's entitlement calculation, a refund's commission reversal) resolves
 * both rates through {@link #rateForVersion}/{@link #holdbackRateForVersion} against those stored
 * ids, reproducing exactly what applied at the time, per COM-02's "never retrospective" and
 * COM-05's "reproduce ... for any historical order" — extended by BRD section 15.2 to holdback
 * percentage explicitly.
 */
public interface PricingConfiguration {

    CommissionRate currentCommissionRate();

    /** @throws com.jamlapps.meridian.web.ApiException NOT_FOUND if the version id is unknown */
    CommissionRate rateForVersion(UUID configurationVersionId);

    HoldbackRate currentHoldbackRate();

    /** @throws com.jamlapps.meridian.web.ApiException NOT_FOUND if the version id is unknown */
    HoldbackRate holdbackRateForVersion(UUID configurationVersionId);
}
