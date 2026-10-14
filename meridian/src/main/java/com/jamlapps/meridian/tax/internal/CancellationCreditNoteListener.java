package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.order.events.SubOrderCancelled;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** REG-08: only when {@code paymentWasConfirmed} — an unconfirmed sub-order was never invoiced, so there is nothing to credit. */
@Component
class CancellationCreditNoteListener {

    private final TaxService tax;

    CancellationCreditNoteListener(TaxService tax) {
        this.tax = tax;
    }

    @ApplicationModuleListener
    void on(SubOrderCancelled event) {
        if (!event.paymentWasConfirmed()) {
            return;
        }

        tax.generateCreditNote(
                event.parentOrderId(), event.refundAmount(), "Buyer cancellation", CreditNoteEntity.ReferenceType.CANCELLATION);
    }
}
