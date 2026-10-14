package com.jamlapps.meridian.fulfilment;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Delivery(
        UUID id,
        UUID subOrderId,
        UUID parentOrderId,
        UUID vendorId,
        DeliveryStatus status,
        @Nullable UUID carrierId,
        @Nullable UUID zoneId,
        @Nullable DeliveryMethod method,
        @Nullable String trackingReference,
        @Nullable String proofOfDeliveryReference,
        Instant createdAt,
        @Nullable Instant dispatchedAt,
        @Nullable Instant deliveredAt) {}
