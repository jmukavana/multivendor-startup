package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.Return;
import com.jamlapps.meridian.returns.ReturnReason;
import com.jamlapps.meridian.returns.ReturnStatus;
import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-RTN-01/02/04. */
@Entity
@Table(name = "return_request")
class ReturnEntity {

    /** BR-RTN-01's "configurable window" — indicative from context, not yet a configured value (OPEN-05-style), matching {@code PayoutDetailEntity.COOLING_OFF}. */
    static final Duration RETURN_WINDOW = Duration.ofDays(7);

    /** BR-RTN-03's "escalation... on timeout" — same indicative treatment. */
    static final Duration VENDOR_RESPONSE_WINDOW = Duration.ofHours(48);

    @Id
    private UUID id;

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "parent_order_id", nullable = false)
    private UUID parentOrderId;

    @Column(name = "buyer_account_id", nullable = false)
    private UUID buyerAccountId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnReason reason;

    @Column
    private @Nullable String description;

    @Column(name = "refund_amount_minor_units", nullable = false)
    private long refundAmountMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReturnStatus status;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    @OneToMany(mappedBy = "returnRequest", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<ReturnImageEntity> images = new ArrayList<>();

    protected ReturnEntity() {
        // for JPA
    }

    ReturnEntity(
            UUID id,
            UUID subOrderId,
            UUID parentOrderId,
            UUID buyerAccountId,
            UUID vendorId,
            ReturnReason reason,
            @Nullable String description,
            Money refundAmount,
            Instant requestedAt) {
        this.id = id;
        this.subOrderId = subOrderId;
        this.parentOrderId = parentOrderId;
        this.buyerAccountId = buyerAccountId;
        this.vendorId = vendorId;
        this.reason = reason;
        this.description = description;
        this.refundAmountMinorUnits = refundAmount.minorUnits();
        this.currency = refundAmount.currency().getCurrencyCode();
        this.status = ReturnStatus.REQUESTED;
        this.requestedAt = requestedAt;
    }

    void addImage(UUID imageId, String blobReference, String contentType, Instant now) {
        images.add(new ReturnImageEntity(imageId, this, blobReference, contentType, now));
    }

    void resolve(ReturnStatus outcome, Instant now) {
        this.status = outcome;
        this.resolvedAt = now;
    }

    void escalate() {
        this.status = ReturnStatus.ESCALATED;
    }

    UUID id() {
        return id;
    }

    UUID subOrderId() {
        return subOrderId;
    }

    UUID parentOrderId() {
        return parentOrderId;
    }

    UUID buyerAccountId() {
        return buyerAccountId;
    }

    UUID vendorId() {
        return vendorId;
    }

    Money refundAmount() {
        return Money.of(refundAmountMinorUnits, Currency.getInstance(currency));
    }

    ReturnStatus status() {
        return status;
    }

    Instant requestedAt() {
        return requestedAt;
    }

    Return toReturn() {
        return new Return(
                id,
                subOrderId,
                parentOrderId,
                buyerAccountId,
                vendorId,
                reason,
                description,
                images.stream().map(ReturnImageEntity::blobReference).toList(),
                refundAmount(),
                status,
                requestedAt,
                resolvedAt);
    }
}
