package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.returns.events.ReturnRefunded;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** REG-08: a return the vendor accepted, or a dispute an admin resolved for the buyer, always follows a confirmed payment — see {@code ReturnService.request}'s own BR-RTN-01 precondition. */
@Component
class ReturnCreditNoteListener {

    private final TaxService tax;

    ReturnCreditNoteListener(TaxService tax) {
        this.tax = tax;
    }

    @ApplicationModuleListener
    void on(ReturnRefunded event) {
        tax.generateCreditNote(event.parentOrderId(), event.refundAmount(), "Return", CreditNoteEntity.ReferenceType.RETURN);
    }
}
