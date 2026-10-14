package com.jamlapps.meridian.pricing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * BR-ADM-06, BR-VND-09: a holdback rate and the configuration version that produced it — the same
 * versioned-value shape as {@link CommissionRate}, for the same reason (COM-05's reasoning applied
 * to the value BRD section 15.2 names alongside commission rate: "reconstructible as at any past
 * order"). {@code rate} is a multiplier — 10% is {@code 0.10}.
 */
public record HoldbackRate(UUID configurationVersionId, BigDecimal rate) {}
