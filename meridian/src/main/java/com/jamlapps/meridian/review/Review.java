package com.jamlapps.meridian.review;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Review(
        UUID id,
        UUID productId,
        UUID vendorId,
        UUID buyerAccountId,
        int rating,
        @Nullable String comment,
        @Nullable String vendorResponse,
        ReviewStatus status,
        @Nullable String removalReason,
        Instant createdAt) {}
