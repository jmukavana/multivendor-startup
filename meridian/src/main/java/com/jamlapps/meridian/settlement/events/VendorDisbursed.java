package com.jamlapps.meridian.settlement.events;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;

/** BR-STL-06: a settlement line's disbursement was accepted by the provider and posted to the ledger. */
public record VendorDisbursed(
        UUID settlementRunId, UUID settlementLineId, UUID vendorId, Money amount, String disbursementReference, Instant disbursedAt) {}
