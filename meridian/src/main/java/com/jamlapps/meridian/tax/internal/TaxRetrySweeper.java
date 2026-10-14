package com.jamlapps.meridian.tax.internal;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * REG-05: retries a FAILED invoice or credit note without anything driving it from outside —
 * structured exactly like {@code settlement.SettlementRetrySweeper} and {@code
 * returns.ReturnTimeoutSweeper}, calling through the injected, proxied {@link TaxService} rather
 * than looping from inside that class.
 */
@Component
class TaxRetrySweeper {

    private static final Logger log = LoggerFactory.getLogger(TaxRetrySweeper.class);

    private final TaxService tax;

    TaxRetrySweeper(TaxService tax) {
        this.tax = tax;
    }

    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        List<UUID> invoiceIds = tax.retryableInvoiceIds();
        for (UUID invoiceId : invoiceIds) {
            tax.transmitInvoice(invoiceId);
        }

        List<UUID> creditNoteIds = tax.retryableCreditNoteIds();
        for (UUID creditNoteId : creditNoteIds) {
            tax.transmitCreditNote(creditNoteId);
        }

        if (!invoiceIds.isEmpty() || !creditNoteIds.isEmpty()) {
            log.info("Tax retry sweep attempted {} invoice(s) and {} credit note(s)", invoiceIds.size(), creditNoteIds.size());
        }
    }
}
