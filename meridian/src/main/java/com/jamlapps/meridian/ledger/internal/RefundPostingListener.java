package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.returns.events.ReturnRefunded;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-RTN-05: posts the commission-reversal contra entry for a return- or dispute-driven refund. */
@Component
class RefundPostingListener {

    private final LedgerService ledger;

    RefundPostingListener(LedgerService ledger) {
        this.ledger = ledger;
    }

    @ApplicationModuleListener
    void on(ReturnRefunded event) {
        ledger.postRefund(
                event.subOrderId(), event.vendorId(), event.refundAmount(), event.commissionRate(), event.holdbackRate(), "RETURN");
    }
}
