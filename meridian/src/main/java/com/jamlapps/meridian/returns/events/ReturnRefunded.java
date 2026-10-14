package com.jamlapps.meridian.returns.events;

import com.jamlapps.meridian.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A refund was issued — a return the vendor accepted, or a dispute an admin resolved for the
 * buyer. Carries the resolved commission rate and holdback rate directly (not just {@code
 * commissionConfigurationId}/{@code holdbackConfigurationId}) for the same reason {@code
 * order.events.OrderPaymentConfirmed} does: {@code ledger} depends only on this module's events,
 * not on {@code pricing}, so this event has to be self-contained for {@code
 * ledger.internal.RefundPostingListener} to post BR-RTN-05's commission and holdback reversal
 * without a further module dependency.
 */
public record ReturnRefunded(
        UUID returnId,
        UUID subOrderId,
        UUID vendorId,
        UUID parentOrderId,
        Money refundAmount,
        BigDecimal commissionRate,
        UUID commissionConfigurationId,
        BigDecimal holdbackRate,
        UUID holdbackConfigurationId,
        Instant refundedAt) {}
