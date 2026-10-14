package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.settlement.SettlementRun;
import com.jamlapps.meridian.settlement.SettlementRunStatus;
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

@Entity
@Table(name = "settlement_run")
class SettlementRunEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementRunStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "approved_by_account_id")
    private @Nullable UUID approvedByAccountId;

    @Column(name = "approved_at")
    private @Nullable Instant approvedAt;

    protected SettlementRunEntity() {
        // for JPA
    }

    SettlementRunEntity(UUID id, Currency currency, SettlementRunStatus status, Instant createdAt) {
        this.id = id;
        this.currency = currency.getCurrencyCode();
        this.status = status;
        this.createdAt = createdAt;
    }

    /** BR-STL-11: DRAFT only — see {@code SettlementService.approve} for why that is checked before this is called. */
    void approve(UUID actorAccountId, Instant now) {
        this.status = SettlementRunStatus.APPROVED;
        this.approvedByAccountId = actorAccountId;
        this.approvedAt = now;
    }

    void complete() {
        this.status = SettlementRunStatus.COMPLETED;
    }

    UUID id() {
        return id;
    }

    Currency currency() {
        return Currency.getInstance(currency);
    }

    SettlementRunStatus status() {
        return status;
    }

    SettlementRun toSettlementRun() {
        return new SettlementRun(id, currency(), status, createdAt, approvedByAccountId, approvedAt);
    }
}
