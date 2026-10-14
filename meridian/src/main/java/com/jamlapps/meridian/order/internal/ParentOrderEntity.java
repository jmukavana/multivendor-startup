package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.OrderStatus;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.RequestOrigin;
import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "parent_order")
class ParentOrderEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String reference;

    @Column(name = "buyer_account_id", nullable = false)
    private UUID buyerAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_minor_units", nullable = false)
    private long totalMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "source_address")
    private @Nullable String sourceAddress;

    @Column(name = "user_agent")
    private @Nullable String userAgent;

    @OneToMany(mappedBy = "parentOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<SubOrderEntity> subOrders = new ArrayList<>();

    protected ParentOrderEntity() {
        // for JPA
    }

    ParentOrderEntity(UUID id, String reference, UUID buyerAccountId, Currency currency, RequestOrigin origin, Instant now) {
        this.id = id;
        this.reference = reference;
        this.buyerAccountId = buyerAccountId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.totalMinorUnits = 0L;
        this.currency = currency.getCurrencyCode();
        this.sourceAddress = origin.sourceAddress();
        this.userAgent = origin.userAgent();
        this.createdAt = now;
        this.updatedAt = now;
    }

    SubOrderEntity addSubOrder(
            UUID vendorId,
            Currency currency,
            UUID commissionConfigurationId,
            UUID holdbackConfigurationId,
            Money deliveryCost,
            Instant now) {
        SubOrderEntity subOrder = new SubOrderEntity(
                UUID.randomUUID(), this, vendorId, currency, commissionConfigurationId, holdbackConfigurationId, deliveryCost, now);
        subOrders.add(subOrder);
        return subOrder;
    }

    /**
     * Recomputes the parent total from its sub-orders. Called once after every sub-order and line
     * is added, rather than accumulated incrementally alongside {@code addSubOrder}/{@code
     * addLine}, so there is exactly one place the invariant "total equals the sum of subtotals"
     * can be violated, and it is this one. BR-CHK-06/07: includes each sub-order's delivery cost,
     * not just its items — {@code SubOrder.total()} is the same items-plus-delivery sum, per
     * sub-order rather than across the whole order.
     */
    void recalculateTotal() {
        this.totalMinorUnits =
                subOrders.stream().mapToLong(s -> s.subtotal().plus(s.deliveryCost()).minorUnits()).sum();
    }

    void markConfirmed(Instant now, Instant acceptanceDeadline) {
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = now;
        subOrders.forEach(subOrder -> subOrder.markConfirmed(now, acceptanceDeadline));
    }

    UUID id() {
        return id;
    }

    String reference() {
        return reference;
    }

    UUID buyerAccountId() {
        return buyerAccountId;
    }

    OrderStatus status() {
        return status;
    }

    List<SubOrderEntity> subOrders() {
        return subOrders;
    }

    @Nullable String sourceAddress() {
        return sourceAddress;
    }

    ParentOrder toParentOrder() {
        return new ParentOrder(
                id,
                reference,
                buyerAccountId,
                status,
                Money.of(totalMinorUnits, Currency.getInstance(currency)),
                subOrders.stream().map(SubOrderEntity::toSubOrder).toList(),
                createdAt,
                sourceAddress,
                userAgent);
    }
}
