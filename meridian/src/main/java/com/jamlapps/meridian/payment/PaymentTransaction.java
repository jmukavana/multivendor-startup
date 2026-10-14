package com.jamlapps.meridian.payment;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record PaymentTransaction(
        UUID id,
        UUID parentOrderId,
        Money amount,
        PaymentStatus status,
        @Nullable String providerTransactionReference,
        Instant initiatedAt) {}
