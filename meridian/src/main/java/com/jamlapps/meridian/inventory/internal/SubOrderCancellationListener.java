package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.order.events.SubOrderCancelled;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-ORD-13/14. See {@code InventoryService.cancelLine}. */
@Component
class SubOrderCancellationListener {

    private final InventoryService inventory;

    SubOrderCancellationListener(InventoryService inventory) {
        this.inventory = inventory;
    }

    @ApplicationModuleListener
    void on(SubOrderCancelled event) {
        for (var line : event.lines()) {
            inventory.cancelLine(event.parentOrderId(), line.productId(), line.quantity());
        }
    }
}
