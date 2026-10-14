package com.jamlapps.meridian.fulfilment;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;

/** BR-FUL-08, DAT-07: versioned — never updated in place, an effective-from timestamp instead. */
public record DeliveryRateCard(UUID id, UUID carrierId, UUID zoneId, DeliveryMethod method, Money price, Instant effectiveFrom) {}
