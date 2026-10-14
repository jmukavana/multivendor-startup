package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.tax.CreditNote;
import com.jamlapps.meridian.tax.TaxDocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** REG-08. */
@Entity
@Table(name = "credit_note")
class CreditNoteEntity {

    static final int MAX_ATTEMPTS = 3;

    enum ReferenceType {
        CANCELLATION,
        RETURN
    }

    @Id
    private UUID id;

    @Column(name = "tax_invoice_id", nullable = false)
    private UUID taxInvoiceId;

    @Column(name = "parent_order_id", nullable = false)
    private UUID parentOrderId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private ReferenceType referenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaxDocumentStatus status;

    @Column(name = "acknowledgement_reference")
    private @Nullable String acknowledgementReference;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "transmitted_at")
    private @Nullable Instant transmittedAt;

    protected CreditNoteEntity() {
        // for JPA
    }

    CreditNoteEntity(
            UUID id, UUID taxInvoiceId, UUID parentOrderId, Money amount, String reason, ReferenceType referenceType, Instant createdAt) {
        this.id = id;
        this.taxInvoiceId = taxInvoiceId;
        this.parentOrderId = parentOrderId;
        this.amountMinorUnits = amount.minorUnits();
        this.currency = amount.currency().getCurrencyCode();
        this.reason = reason;
        this.referenceType = referenceType;
        this.status = TaxDocumentStatus.PENDING;
        this.createdAt = createdAt;
    }

    void recordSuccess(String acknowledgementReference, Instant now) {
        this.status = TaxDocumentStatus.TRANSMITTED;
        this.acknowledgementReference = acknowledgementReference;
        this.transmittedAt = now;
    }

    void recordFailure() {
        this.attemptCount++;
        this.status = attemptCount >= MAX_ATTEMPTS ? TaxDocumentStatus.DEAD_LETTERED : TaxDocumentStatus.FAILED;
    }

    UUID id() {
        return id;
    }

    UUID taxInvoiceId() {
        return taxInvoiceId;
    }

    Money amount() {
        return Money.of(amountMinorUnits, Currency.getInstance(currency));
    }

    TaxDocumentStatus status() {
        return status;
    }

    CreditNote toCreditNote() {
        return new CreditNote(
                id, taxInvoiceId, parentOrderId, amount(), reason, status, acknowledgementReference, attemptCount, createdAt, transmittedAt);
    }
}
