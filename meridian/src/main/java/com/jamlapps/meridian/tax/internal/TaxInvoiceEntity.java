package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.tax.TaxDocumentStatus;
import com.jamlapps.meridian.tax.TaxInvoice;
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

/** REG-04/05/07. */
@Entity
@Table(name = "tax_invoice")
class TaxInvoiceEntity {

    static final int MAX_ATTEMPTS = 3;

    @Id
    private UUID id;

    @Column(name = "parent_order_id", nullable = false)
    private UUID parentOrderId;

    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false)
    private String currency;

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

    protected TaxInvoiceEntity() {
        // for JPA
    }

    TaxInvoiceEntity(UUID id, UUID parentOrderId, String invoiceNumber, Money amount, Instant createdAt) {
        this.id = id;
        this.parentOrderId = parentOrderId;
        this.invoiceNumber = invoiceNumber;
        this.amountMinorUnits = amount.minorUnits();
        this.currency = amount.currency().getCurrencyCode();
        this.status = TaxDocumentStatus.PENDING;
        this.createdAt = createdAt;
    }

    void recordSuccess(String acknowledgementReference, Instant now) {
        this.status = TaxDocumentStatus.TRANSMITTED;
        this.acknowledgementReference = acknowledgementReference;
        this.transmittedAt = now;
    }

    /** REG-05: retried up to {@link #MAX_ATTEMPTS} before REG-06's dead letter. */
    void recordFailure() {
        this.attemptCount++;
        this.status = attemptCount >= MAX_ATTEMPTS ? TaxDocumentStatus.DEAD_LETTERED : TaxDocumentStatus.FAILED;
    }

    UUID id() {
        return id;
    }

    UUID parentOrderId() {
        return parentOrderId;
    }

    String invoiceNumber() {
        return invoiceNumber;
    }

    Money amount() {
        return Money.of(amountMinorUnits, Currency.getInstance(currency));
    }

    TaxDocumentStatus status() {
        return status;
    }

    TaxInvoice toTaxInvoice() {
        return new TaxInvoice(id, parentOrderId, invoiceNumber, amount(), status, acknowledgementReference, attemptCount, createdAt, transmittedAt);
    }
}
