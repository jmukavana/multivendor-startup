package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.order.events.OrderPaymentConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-INV-05, AC-INV-03. See {@code InventoryService.confirmForOrder}. */
@Component
class OrderPaymentConfirmationListener {

    private final InventoryService inventory;

    OrderPaymentConfirmationListener(InventoryService inventory) {
        this.inventory = inventory;
    }

    @ApplicationModuleListener
    void on(OrderPaymentConfirmed event) {
        inventory.confirmForOrder(event.parentOrderId());
    }
}
