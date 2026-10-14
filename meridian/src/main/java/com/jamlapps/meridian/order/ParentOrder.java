package com.jamlapps.meridian.order;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A buyer's order, as other modules — and, via {@code OrderController}, the frontend — see it.
 *
 * <p>Always exactly one sub-order in the walking skeleton, because checkout does not yet split a
 * cart across vendors (that is Phase 4, BR-CHK). The list is here now, not a single {@code SubOrder}
 * field, so that Phase 4 extends this type rather than replacing every caller's shape.
 */
public record ParentOrder(
        UUID id,
        String reference,
        UUID buyerAccountId,
        OrderStatus status,
        Money total,
        List<SubOrder> subOrders,
        Instant createdAt,
        /** BR-SEC-03: the checkout request's own origin — null only for an order this codebase's own test fixtures create without one. */
        @Nullable String sourceAddress,
        @Nullable String userAgent) {

    public ParentOrder {
        subOrders = List.copyOf(subOrders);
    }
}
