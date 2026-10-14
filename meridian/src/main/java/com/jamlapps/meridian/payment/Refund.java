package com.jamlapps.meridian.payment;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record Refund(
        UUID id,
        UUID paymentTransactionId,
        Money amount,
        String reason,
        RefundStatus status,
        @Nullable String providerRefundReference,
        Instant initiatedAt) {}
