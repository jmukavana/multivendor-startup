package com.jamlapps.meridian.review;

import java.math.BigDecimal;

/** BR-REV-03: aggregated to product level and, separately, vendor level — the same shape either way, so one type serves both. */
public record RatingSummary(BigDecimal averageRating, long reviewCount) {}
