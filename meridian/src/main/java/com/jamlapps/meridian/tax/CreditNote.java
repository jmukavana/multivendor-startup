package com.jamlapps.meridian.tax;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** REG-08: references the {@link TaxInvoice} it corrects. */
public record CreditNote(
        UUID id,
        UUID taxInvoiceId,
        UUID parentOrderId,
        Money amount,
        String reason,
        TaxDocumentStatus status,
        @Nullable String acknowledgementReference,
        int attemptCount,
        Instant createdAt,
        @Nullable Instant transmittedAt) {}
