package com.jamlapps.meridian.pricing;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A commission rate and the configuration version that produced it (COM-05). {@code rate} is a
 * multiplier — 9% is {@code 0.09}, matching {@code shared.Money.percentage}'s own convention.
 */
public record CommissionRate(UUID configurationVersionId, BigDecimal rate) {}
