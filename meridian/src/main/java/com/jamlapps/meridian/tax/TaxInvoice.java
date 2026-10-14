package com.jamlapps.meridian.tax;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** REG-04/07: one per parent order — see this module's package doc on why per-order, not per-sub-order. */
public record TaxInvoice(
        UUID id,
        UUID parentOrderId,
        String invoiceNumber,
        Money amount,
        TaxDocumentStatus status,
        @Nullable String acknowledgementReference,
        int attemptCount,
        Instant createdAt,
        @Nullable Instant transmittedAt) {}
