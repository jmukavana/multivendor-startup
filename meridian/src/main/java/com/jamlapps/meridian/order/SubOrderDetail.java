package com.jamlapps.meridian.order;

import com.jamlapps.meridian.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A sub-order as {@code returns} needs to see it — the fields {@link SubOrder} (the buyer-facing,
 * JSON-serialised shape) does not carry: {@code parentOrderId} and {@code buyerAccountId}, needed
 * to call {@code payment.RefundInitiation} and to authorise a return request; and
 * {@code commissionRate}/{@code holdbackRate}, already resolved here (COM-05 and BR-ADM-06, the
 * same rates {@code order} resolved for itself in {@code OrderService.markPaymentConfirmed}) so
 * {@code returns} never needs a {@code pricing} dependency of its own just to reconstruct them —
 * matching {@code OrderPaymentConfirmed.SubOrderSettlementDetail}'s reasoning exactly, one module
 * over.
 */
public record SubOrderDetail(
        UUID subOrderId,
        UUID parentOrderId,
        UUID vendorId,
        UUID buyerAccountId,
        OrderStatus status,
        Money subtotal,
        List<OrderLine> lines,
        BigDecimal commissionRate,
        UUID commissionConfigurationId,
        BigDecimal holdbackRate,
        UUID holdbackConfigurationId,
        @Nullable Instant deliveredAt) {

    public SubOrderDetail {
        lines = List.copyOf(lines);
    }
}
