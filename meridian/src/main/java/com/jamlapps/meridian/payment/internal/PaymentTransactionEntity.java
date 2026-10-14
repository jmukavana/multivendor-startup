package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentStatus;
import com.jamlapps.meridian.payment.PaymentTransaction;
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

@Entity
@Table(name = "payment_transaction")
class PaymentTransactionEntity {

    @Id
    private UUID id;

    @Column(name = "parent_order_id", nullable = false)
    private UUID parentOrderId;

    @Column(name = "amount_minor_units", nullable = false)
    private long amountMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(name = "buyer_account_id", nullable = false)
    private UUID buyerAccountId;

    @Column(name = "buyer_phone_number", nullable = false)
    private String buyerPhoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "gateway_reference")
    private @Nullable String gatewayReference;

    @Column(name = "provider_transaction_reference")
    private @Nullable String providerTransactionReference;

    @Column(name = "initiated_at", nullable = false)
    private Instant initiatedAt;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    protected PaymentTransactionEntity() {
        // for JPA
    }

    PaymentTransactionEntity(
            UUID id, UUID parentOrderId, Money amount, UUID buyerAccountId, String buyerPhoneNumber, Instant initiatedAt) {
        this.id = id;
        this.parentOrderId = parentOrderId;
        this.amountMinorUnits = amount.minorUnits();
        this.currency = amount.currency().getCurrencyCode();
        this.buyerAccountId = buyerAccountId;
        this.buyerPhoneNumber = buyerPhoneNumber;
        this.status = PaymentStatus.INITIATED;
        this.initiatedAt = initiatedAt;
    }

    void markAwaitingConfirmation(String gatewayReference) {
        this.status = PaymentStatus.AWAITING_CONFIRMATION;
        this.gatewayReference = gatewayReference;
    }

    // No confirm()/fail() mutators: BR-PAY-05's idempotency guarantee lives in
    // PaymentTransactionRepository's atomic tryConfirm/tryFail queries, not in Java-level
    // read-then-write — see that repository's javadoc for why a mutator here would reintroduce
    // exactly the race those queries exist to close.

    Money amount() {
        return Money.of(amountMinorUnits, Currency.getInstance(currency));
    }

    PaymentTransaction toPaymentTransaction() {
        return new PaymentTransaction(id, parentOrderId, amount(), status, providerTransactionReference, initiatedAt);
    }

    UUID id() {
        return id;
    }

    UUID parentOrderId() {
        return parentOrderId;
    }

    UUID buyerAccountId() {
        return buyerAccountId;
    }

    PaymentStatus status() {
        return status;
    }

    @Nullable String gatewayReference() {
        return gatewayReference;
    }
}
