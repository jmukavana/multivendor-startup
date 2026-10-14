package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-LDG-04: no update, no delete — every field here is set once, at construction. */
@Entity
@Table(name = "ledger_entry")
class LedgerEntryEntity {

    @Id
    private UUID id;

    @Column(name = "posting_id", nullable = false)
    private UUID postingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LedgerAccount account;

    @Column(name = "vendor_id")
    private @Nullable UUID vendorId;

    @Column(name = "is_debit", nullable = false)
    private boolean debit;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(name = "reference_type", nullable = false)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    protected LedgerEntryEntity() {
        // for JPA
    }

    LedgerEntryEntity(
            UUID id,
            UUID postingId,
            LedgerAccount account,
            @Nullable UUID vendorId,
            boolean debit,
            Money amount,
            String referenceType,
            UUID referenceId,
            Instant postedAt) {
        this.id = id;
        this.postingId = postingId;
        this.account = account;
        this.vendorId = vendorId;
        this.debit = debit;
        this.amountMinorUnits = amount.minorUnits();
        this.currency = amount.currency().getCurrencyCode();
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.postedAt = postedAt;
    }

    long signedAmount() {
        return debit ? amountMinorUnits : -amountMinorUnits;
    }

    boolean isDebit() {
        return debit;
    }

    long amountMinorUnits() {
        return amountMinorUnits;
    }

    LedgerAccount account() {
        return account;
    }

    @Nullable UUID vendorId() {
        return vendorId;
    }
}
