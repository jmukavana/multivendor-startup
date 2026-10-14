package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.settlement.SettlementLine;
import com.jamlapps.meridian.settlement.SettlementLineStatus;
import com.jamlapps.meridian.shared.Money;
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

/**
 * One vendor's line within one run. BR-STL-12's idempotency key is set once, at construction, and
 * never changes — see {@code V14__settlement.sql}'s comment on why that stability is the point.
 */
@Entity
@Table(name = "settlement_line")
class SettlementLineEntity {

    static final int MAX_ATTEMPTS = 3;

    @Id
    private UUID id;

    @Column(name = "settlement_run_id", nullable = false)
    private UUID settlementRunId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SettlementLineStatus status;

    @Column(name = "exclusion_reason")
    private @Nullable String exclusionReason;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "disbursement_reference")
    private @Nullable String disbursementReference;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    protected SettlementLineEntity() {
        // for JPA
    }

    private SettlementLineEntity(
            UUID id, UUID settlementRunId, UUID vendorId, Money amount, SettlementLineStatus status, @Nullable String exclusionReason) {
        this.id = id;
        this.settlementRunId = settlementRunId;
        this.vendorId = vendorId;
        this.amountMinorUnits = amount.minorUnits();
        this.currency = amount.currency().getCurrencyCode();
        this.status = status;
        this.exclusionReason = exclusionReason;
        this.idempotencyKey = UUID.randomUUID().toString();
    }

    static SettlementLineEntity pending(UUID id, UUID settlementRunId, UUID vendorId, Money amount) {
        return new SettlementLineEntity(id, settlementRunId, vendorId, amount, SettlementLineStatus.PENDING, null);
    }

    static SettlementLineEntity excluded(UUID id, UUID settlementRunId, UUID vendorId, Money amount, String reason) {
        return new SettlementLineEntity(id, settlementRunId, vendorId, amount, SettlementLineStatus.EXCLUDED, reason);
    }

    /** BR-STL-07: a failed attempt, retried up to {@link #MAX_ATTEMPTS} before escalating. */
    void recordFailure() {
        this.attemptCount++;
        this.status = attemptCount >= MAX_ATTEMPTS ? SettlementLineStatus.ESCALATED : SettlementLineStatus.FAILED;
    }

    void recordSuccess(String disbursementReference, Instant now) {
        this.status = SettlementLineStatus.DISBURSED;
        this.disbursementReference = disbursementReference;
        this.resolvedAt = now;
    }

    UUID id() {
        return id;
    }

    UUID vendorId() {
        return vendorId;
    }

    UUID settlementRunId() {
        return settlementRunId;
    }

    Money amount() {
        return Money.of(amountMinorUnits, Currency.getInstance(currency));
    }

    SettlementLineStatus status() {
        return status;
    }

    String idempotencyKey() {
        return idempotencyKey;
    }

    int attemptCount() {
        return attemptCount;
    }

    @Nullable Instant resolvedAt() {
        return resolvedAt;
    }

    SettlementLine toSettlementLine() {
        return new SettlementLine(id, settlementRunId, vendorId, amount(), status, exclusionReason, attemptCount, disbursementReference);
    }
}
