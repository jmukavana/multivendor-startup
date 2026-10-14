package com.jamlapps.meridian.returns;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Return(
        UUID id,
        UUID subOrderId,
        UUID parentOrderId,
        UUID buyerAccountId,
        UUID vendorId,
        ReturnReason reason,
        @Nullable String description,
        List<String> imageReferences,
        Money refundAmount,
        ReturnStatus status,
        Instant requestedAt,
        @Nullable Instant resolvedAt) {

    public Return {
        imageReferences = List.copyOf(imageReferences);
    }
}
