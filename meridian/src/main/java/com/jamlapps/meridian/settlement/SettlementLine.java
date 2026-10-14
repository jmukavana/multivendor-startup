package com.jamlapps.meridian.settlement;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SettlementLine(
        UUID id,
        UUID settlementRunId,
        UUID vendorId,
        Money amount,
        SettlementLineStatus status,
        @Nullable String exclusionReason,
        int attemptCount,
        @Nullable String disbursementReference) {}
