package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.Refund;
import com.jamlapps.meridian.payment.RefundStatus;
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

/** BR-PAY-11, append-only in spirit: no mutator besides the constructor, matching PaymentTransactionEntity's own reasoning — {@link MockRefundGateway} resolves synchronously, so there is no pending state to transition out of later. */
@Entity
@Table(name = "refund")
class RefundEntity {

    @Id
    private UUID id;

    @Column(name = "payment_transaction_id", nullable = false)
    private UUID paymentTransactionId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RefundStatus status;

    @Column(name = "provider_refund_reference")
    private @Nullable String providerRefundReference;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    protected RefundEntity() {
        // for JPA
    }

    RefundEntity(
            UUID id,
            UUID paymentTransactionId,
            Money amount,
            String reason,
            RefundStatus status,
            @Nullable String providerRefundReference,
            Instant initiatedAt,
            @Nullable Instant resolvedAt) {
        this.id = id;
        this.paymentTransactionId = paymentTransactionId;
        this.amountMinorUnits = amount.minorUnits();
        this.currency = amount.currency().getCurrencyCode();
        this.reason = reason;
        this.status = status;
        this.providerRefundReference = providerRefundReference;
        this.initiatedAt = initiatedAt;
        this.resolvedAt = resolvedAt;
    }

    Money amount() {
        return Money.of(amountMinorUnits, Currency.getInstance(currency));
    }

    Refund toRefund() {
        return new Refund(id, paymentTransactionId, amount(), reason, status, providerRefundReference, initiatedAt);
    }
}
