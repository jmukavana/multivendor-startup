package com.jamlapps.meridian.fulfilment;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

/** BR-CHK-06: the cheapest active carrier's current rate for one zone and method, as {@link DeliveryQuotes} resolves it. */
public record DeliveryQuote(UUID carrierId, UUID rateCardId, Money price) {}
