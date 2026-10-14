package com.jamlapps.meridian.fulfilment;

import java.util.UUID;

/**
 * BR-CHK-05/06: how {@code checkout} records the buyer's delivery choice for one sub-order,
 * before a {@link Delivery} row can exist for it — {@code fulfilment.internal
 * .OrderReadinessListener} creates that only once the order's payment is confirmed. Separated from
 * the read-only {@link DeliveryQuotes} deliberately, matching {@code order.OrderCreation}'s own
 * split from the read-only {@code Orders} — this is the first genuinely synchronous write {@code
 * checkout} makes into this module.
 */
public interface DeliverySelections {

    /**
     * @param carrierId, {@code zoneId}, {@code method} and {@code rateCardId} together fix exactly
     *     which quote is honoured once the sub-order is actually booked — never re-resolved against
     *     whatever rate happens to be current later, the same versioned-value discipline {@code
     *     order} already applies to the commission rate it captures at creation (COM-02, DAT-07).
     */
    void select(UUID subOrderId, UUID carrierId, UUID zoneId, DeliveryMethod method, UUID rateCardId);
}
