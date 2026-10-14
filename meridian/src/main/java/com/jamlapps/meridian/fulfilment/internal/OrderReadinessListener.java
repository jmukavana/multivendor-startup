package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.order.events.OrderPaymentConfirmed;
import java.time.Clock;
import java.util.UUID;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-FUL-01: creates one {@code Delivery} per confirmed sub-order.
 *
 * <p>BR-CHK-05/06: if the buyer made a delivery choice at checkout — {@code
 * DeliverySelectionRepository} has a row for this sub-order, written by {@code checkout} through
 * {@code DeliverySelectionsAdapter} right after the order was created — that choice is honoured
 * immediately: the delivery is booked with the buyer's own carrier, zone and method, not left
 * PENDING for a vendor to book by hand. No selection (nothing else in this codebase creates an
 * order any other way, but a future path might) falls back to the original PENDING, unbooked
 * behaviour — a vendor booking it manually through this module's own controller, exactly as
 * before this integration existed.
 *
 * <p>No explicit {@code @Transactional} here: {@code @ApplicationModuleListener} already implies
 * {@code @Async @Transactional(REQUIRES_NEW)} — adding one is a startup-time error, not a
 * redundant no-op (this codebase already hit that once, in catalog's {@code
 * VendorSuspensionListener}).
 */
@Component
class OrderReadinessListener {

    private final DeliveryRepository deliveries;
    private final DeliverySelectionRepository selections;
    private final DeliveryService deliveryService;
    private final Clock clock;

    OrderReadinessListener(
            DeliveryRepository deliveries, DeliverySelectionRepository selections, DeliveryService deliveryService, Clock clock) {
        this.deliveries = deliveries;
        this.selections = selections;
        this.deliveryService = deliveryService;
        this.clock = clock;
    }

    @ApplicationModuleListener
    void on(OrderPaymentConfirmed event) {
        for (var subOrder : event.subOrders()) {
            DeliveryEntity delivery = deliveries.save(new DeliveryEntity(
                    UUID.randomUUID(),
                    subOrder.subOrderId(),
                    event.parentOrderId(),
                    subOrder.vendorId(),
                    event.buyerAccountId(),
                    clock.instant()));

            selections
                    .findBySubOrderId(subOrder.subOrderId())
                    .ifPresent(selection ->
                            deliveryService.autoBook(delivery.id(), selection.carrierId(), selection.zoneId(), selection.method()));
        }
    }
}
