package com.jamlapps.meridian.order;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record SubOrder(
        UUID id,
        UUID vendorId,
        OrderStatus status,
        Money subtotal,
        /** BR-CHK-06/07: resolved once, at checkout, and never recomputed after — see {@code OrderCreation}'s javadoc. */
        Money deliveryCost,
        List<OrderLine> lines,
        /** COM-05: the exact commission configuration version in force when this was created. */
        UUID commissionConfigurationId,
        /** BR-ADM-06: the exact holdback configuration version in force when this was created. */
        UUID holdbackConfigurationId,
        /** BR-ORD-15: whether {@code OrderService.cancel} would currently accept a cancellation of this sub-order. */
        boolean cancellable,
        /** BR-ORD-05: when the vendor's acceptance window closes; null once the sub-order has moved past CONFIRMED. */
        @Nullable Instant acceptanceDeadline,
        Instant createdAt) {

    public SubOrder {
        lines = List.copyOf(lines);
    }

    /** BR-CHK-07: the "total" line of the per-vendor cost breakdown — items plus delivery. */
    public Money total() {
        return subtotal.plus(deliveryCost);
    }
}
