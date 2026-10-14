package com.jamlapps.meridian.settlement;

import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SettlementRun(
        UUID id,
        Currency currency,
        SettlementRunStatus status,
        Instant createdAt,
        @Nullable UUID approvedByAccountId,
        @Nullable Instant approvedAt) {}
