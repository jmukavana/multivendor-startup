package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.order.events.SubOrderCancelled;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-ORD-14: posts the same commission-reversal contra entry {@link RefundPostingListener} posts
 * for a return, triggered by a buyer cancellation instead. Only when {@code
 * event.paymentWasConfirmed()} — a sub-order still PENDING_PAYMENT was never charged, so there is
 * nothing here to reverse.
 */
@Component
class CancellationRefundPostingListener {

    private final LedgerService ledger;

    CancellationRefundPostingListener(LedgerService ledger) {
        this.ledger = ledger;
    }

    @ApplicationModuleListener
    void on(SubOrderCancelled event) {
        if (!event.paymentWasConfirmed()) {
            return;
        }

        ledger.postRefund(
                event.subOrderId(),
                event.vendorId(),
                event.refundAmount(),
                event.commissionRate(),
                event.holdbackRate(),
                "SUB_ORDER_CANCELLATION");
    }
}
