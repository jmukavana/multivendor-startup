package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.tax.CreditNote;
import com.jamlapps.meridian.tax.TaxDocumentStatus;
import com.jamlapps.meridian.tax.TaxInvoice;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REG-04 to 08. Unlike {@code settlement}'s disbursement or {@code payment}'s callback handling,
 * transmission here does not use the atomic-claim pattern ({@code tryStartDisbursing}, {@code
 * tryConfirm}) those use — deliberately: BR-STL-12 requires exactly-once disbursement because a
 * duplicate is a duplicate financial movement, but a duplicate eTIMS transmission is not the same
 * kind of harm, and REG-04 to 08 do not ask for that guarantee. A plain read-check-write inside
 * one {@code @Transactional} method is a genuine, lower-stakes reduction here, not an oversight.
 */
@Service
class TaxService {

    private static final Logger log = LoggerFactory.getLogger(TaxService.class);

    private final TaxInvoiceRepository invoices;
    private final CreditNoteRepository creditNotes;
    private final EtimsGateway gateway;
    private final Clock clock;

    TaxService(TaxInvoiceRepository invoices, CreditNoteRepository creditNotes, EtimsGateway gateway, Clock clock) {
        this.invoices = invoices;
        this.creditNotes = creditNotes;
        this.gateway = gateway;
        this.clock = clock;
    }

    /**
     * REG-04: one invoice per parent order — see this module's package doc on why per-order, not
     * per-sub-order. Calls {@link #transmitInvoice} directly (self-invocation, sharing this
     * method's own transaction) rather than as a separate proxied call — safe here specifically
     * because neither method uses an atomic-claim {@code @Modifying} query the way {@code
     * payment}/{@code settlement} do; self-invocation only becomes a correctness problem when it
     * silently drops a query's transactional requirement, which does not apply to this class.
     */
    @Transactional
    void generateInvoice(UUID parentOrderId, Money amount) {
        TaxInvoiceEntity invoice = invoices.save(
                new TaxInvoiceEntity(UUID.randomUUID(), parentOrderId, TaxInvoiceNumberGenerator.generate(), amount, clock.instant()));
        transmitInvoice(invoice.id());
    }

    /**
     * REG-05: attempts transmission once. Called both from {@link #generateInvoice} (the first
     * attempt) and {@code TaxRetrySweeper} (every retry) — a plain method, not looped from inside
     * this class in the sweeper's case; see {@code settlement.internal.SettlementService}'s class
     * doc for why that distinction matters when a caller loops over many ids.
     */
    @Transactional
    void transmitInvoice(UUID invoiceId) {
        TaxInvoiceEntity invoice = invoices.findById(invoiceId).orElseThrow();

        if (invoice.status() != TaxDocumentStatus.PENDING && invoice.status() != TaxDocumentStatus.FAILED) {
            return;
        }

        try {
            var result = gateway.transmitInvoice(invoice.invoiceNumber(), invoice.amount());
            if (result.accepted()) {
                invoice.recordSuccess(result.acknowledgementReference(), clock.instant());
            } else {
                invoice.recordFailure();
            }
        } catch (RuntimeException e) {
            log.warn("eTIMS transmission failed for invoice {}: {}", invoice.invoiceNumber(), e.getMessage());
            invoice.recordFailure();
        }

        invoices.save(invoice);
    }

    /**
     * REG-08: only ever called once the sub-order's payment was confirmed (see the two listeners
     * that call this) — a tax invoice must already exist by then, so a missing one is treated as a
     * genuine invariant violation, not a normal "not found yet".
     */
    @Transactional
    void generateCreditNote(UUID parentOrderId, Money amount, String reason, CreditNoteEntity.ReferenceType referenceType) {
        TaxInvoiceEntity invoice = invoices
                .findByParentOrderId(parentOrderId)
                .orElseThrow(() -> new IllegalStateException(
                        "No tax invoice exists for order " + parentOrderId + "; a refund should never precede one."));

        CreditNoteEntity creditNote = creditNotes.save(new CreditNoteEntity(
                UUID.randomUUID(), invoice.id(), parentOrderId, amount, reason, referenceType, clock.instant()));

        // Self-invocation, same as generateInvoice -> transmitInvoice — safe for the same reason.
        transmitCreditNote(creditNote.id());
    }

    @Transactional
    void transmitCreditNote(UUID creditNoteId) {
        CreditNoteEntity creditNote = creditNotes.findById(creditNoteId).orElseThrow();

        if (creditNote.status() != TaxDocumentStatus.PENDING && creditNote.status() != TaxDocumentStatus.FAILED) {
            return;
        }

        TaxInvoiceEntity invoice = invoices.findById(creditNote.taxInvoiceId()).orElseThrow();

        try {
            var result = gateway.transmitCreditNote(invoice.invoiceNumber(), creditNote.amount());
            if (result.accepted()) {
                Instant now = clock.instant();
                creditNote.recordSuccess(result.acknowledgementReference(), now);
            } else {
                creditNote.recordFailure();
            }
        } catch (RuntimeException e) {
            log.warn("eTIMS transmission failed for credit note against invoice {}: {}", invoice.invoiceNumber(), e.getMessage());
            creditNote.recordFailure();
        }

        creditNotes.save(creditNote);
    }

    /** REG-07: retrieves the retained invoice for an order — the successful counterpart to {@link #exceptionInvoices}. */
    @Transactional(readOnly = true)
    TaxInvoice getInvoiceByOrder(UUID parentOrderId) {
        return invoices
                .findByParentOrderId(parentOrderId)
                .map(TaxInvoiceEntity::toTaxInvoice)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "No tax invoice for order " + parentOrderId + "."));
    }

    /** REG-07/08: every credit note issued against an order's invoice. */
    @Transactional(readOnly = true)
    List<CreditNote> getCreditNotesByOrder(UUID parentOrderId) {
        TaxInvoiceEntity invoice = invoices
                .findByParentOrderId(parentOrderId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "No tax invoice for order " + parentOrderId + "."));

        return creditNotes.findByTaxInvoiceId(invoice.id()).stream().map(CreditNoteEntity::toCreditNote).toList();
    }

    @Transactional(readOnly = true)
    List<UUID> retryableInvoiceIds() {
        return invoices.findByStatus(TaxDocumentStatus.FAILED).stream().map(TaxInvoiceEntity::id).toList();
    }

    @Transactional(readOnly = true)
    List<UUID> retryableCreditNoteIds() {
        return creditNotes.findByStatus(TaxDocumentStatus.FAILED).stream().map(CreditNoteEntity::id).toList();
    }

    /** REG-06: the admin exception queue — FAILED (still retrying) and DEAD_LETTERED (exhausted) both belong in it. */
    @Transactional(readOnly = true)
    List<TaxInvoice> exceptionInvoices() {
        return concat(invoices.findByStatus(TaxDocumentStatus.FAILED), invoices.findByStatus(TaxDocumentStatus.DEAD_LETTERED)).stream()
                .map(TaxInvoiceEntity::toTaxInvoice)
                .toList();
    }

    @Transactional(readOnly = true)
    List<CreditNote> exceptionCreditNotes() {
        return concat(creditNotes.findByStatus(TaxDocumentStatus.FAILED), creditNotes.findByStatus(TaxDocumentStatus.DEAD_LETTERED)).stream()
                .map(CreditNoteEntity::toCreditNote)
                .toList();
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream()).toList();
    }
}
