package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.order.events.OrderPaymentConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-LDG-02: posts one balanced set of entries per sub-order confirmed. */
@Component
class LedgerPostingListener {

    private final LedgerService ledger;

    LedgerPostingListener(LedgerService ledger) {
        this.ledger = ledger;
    }

    @ApplicationModuleListener
    void on(OrderPaymentConfirmed event) {
        for (var subOrder : event.subOrders()) {
            ledger.postPaymentConfirmed(
                    subOrder.subOrderId(), subOrder.vendorId(), subOrder.subtotal(), subOrder.commissionRate(), subOrder.holdbackRate());
        }
    }
}
