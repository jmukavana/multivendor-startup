package com.jamlapps.meridian.fulfilment;

import java.util.UUID;

/** BR-FUL-08. */
public record DeliveryZone(UUID id, String name, String county, boolean active) {}
