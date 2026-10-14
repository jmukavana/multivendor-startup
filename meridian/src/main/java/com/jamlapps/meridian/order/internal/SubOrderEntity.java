package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.OrderStatus;
import com.jamlapps.meridian.order.SubOrder;
import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "sub_order")
class SubOrderEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "parent_order_id", nullable = false)
    private ParentOrderEntity parentOrder;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "commission_configuration_id", nullable = false)
    private UUID commissionConfigurationId;

    @Column(name = "holdback_configuration_id", nullable = false)
    private UUID holdbackConfigurationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "subtotal_minor_units", nullable = false)
    private long subtotalMinorUnits;

    @Column(name = "delivery_cost_minor_units", nullable = false)
    private long deliveryCostMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "delivered_at")
    private @Nullable Instant deliveredAt;

    @Column(name = "acceptance_deadline")
    private @Nullable Instant acceptanceDeadline;

    // Eager: a sub-order is never meaningfully read without its lines, and the walking skeleton
    // never has more than a handful of them.
    @OneToMany(mappedBy = "subOrder", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id")
    private List<OrderLineEntity> lines = new ArrayList<>();

    protected SubOrderEntity() {
        // for JPA
    }

    SubOrderEntity(
            UUID id,
            ParentOrderEntity parentOrder,
            UUID vendorId,
            Currency currency,
            UUID commissionConfigurationId,
            UUID holdbackConfigurationId,
            Money deliveryCost,
            Instant now) {
        this.id = id;
        this.parentOrder = parentOrder;
        this.vendorId = vendorId;
        this.commissionConfigurationId = commissionConfigurationId;
        this.holdbackConfigurationId = holdbackConfigurationId;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.subtotalMinorUnits = 0L;
        this.deliveryCostMinorUnits = deliveryCost.minorUnits();
        this.currency = currency.getCurrencyCode();
        this.createdAt = now;
        this.updatedAt = now;
    }

    void addLine(UUID productId, String productNameSnapshot, Money unitPrice, int quantity) {
        OrderLineEntity line = new OrderLineEntity(
                UUID.randomUUID(), this, productId, productNameSnapshot, unitPrice, quantity);
        lines.add(line);
        subtotalMinorUnits += line.lineTotal().minorUnits();
    }

    /** BR-ORD-05: {@code acceptanceDeadline} is when the vendor's window to {@link #accept} this closes. */
    void markConfirmed(Instant now, Instant acceptanceDeadline) {
        this.status = OrderStatus.CONFIRMED;
        this.updatedAt = now;
        this.acceptanceDeadline = acceptanceDeadline;
    }

    /**
     * BR-ORD-05. Caller ({@code OrderService.accept}) has already checked the sub-order is
     * CONFIRMED. {@code acceptanceDeadline} is left as-is rather than cleared — stale once past
     * ACCEPTED, the same treatment {@link #deliveredAt} gets once a sub-order moves past DELIVERED.
     */
    void accept(Instant now) {
        this.status = OrderStatus.ACCEPTED;
        this.updatedAt = now;
    }

    /** See {@code OrderStatus}'s javadoc: a vendor-invoked stand-in for Phase 7's real fulfilment trigger. */
    void markDelivered(Instant now) {
        this.status = OrderStatus.DELIVERED;
        this.deliveredAt = now;
        this.updatedAt = now;
    }

    /** BR-ORD-13. Caller ({@code OrderService.cancel}) has already checked {@link #isCancellable()}. */
    void cancel(Instant now) {
        this.status = OrderStatus.CANCELLED;
        this.updatedAt = now;
    }

    /** BR-ORD-13/15: cancellable at any point before DELIVERED — see {@code OrderStatus}'s javadoc on why that stands in for "before Dispatched". */
    boolean isCancellable() {
        return status == OrderStatus.PENDING_PAYMENT || status == OrderStatus.CONFIRMED || status == OrderStatus.ACCEPTED;
    }

    /** BR-ORD-05: whether this sub-order has moved past accepting or auto-cancelling — used by {@code OrderService.autoCancelUnaccepted}. */
    boolean isAwaitingAcceptance() {
        return status == OrderStatus.CONFIRMED;
    }

    Money subtotal() {
        return Money.of(subtotalMinorUnits, Currency.getInstance(currency));
    }

    Money deliveryCost() {
        return Money.of(deliveryCostMinorUnits, Currency.getInstance(currency));
    }

    /** BR-CHK-07: items plus delivery — see {@code SubOrder.total()}, which this mirrors. */
    Money total() {
        return subtotal().plus(deliveryCost());
    }

    UUID id() {
        return id;
    }

    UUID parentOrderId() {
        return parentOrder.id();
    }

    UUID vendorId() {
        return vendorId;
    }

    UUID buyerAccountId() {
        return parentOrder.buyerAccountId();
    }

    OrderStatus status() {
        return status;
    }

    UUID commissionConfigurationId() {
        return commissionConfigurationId;
    }

    UUID holdbackConfigurationId() {
        return holdbackConfigurationId;
    }

    @Nullable Instant deliveredAt() {
        return deliveredAt;
    }

    List<OrderLineEntity> lines() {
        return lines;
    }

    SubOrder toSubOrder() {
        return new SubOrder(
                id,
                vendorId,
                status,
                subtotal(),
                deliveryCost(),
                lines.stream().map(OrderLineEntity::toOrderLine).toList(),
                commissionConfigurationId,
                holdbackConfigurationId,
                isCancellable(),
                acceptanceDeadline,
                createdAt);
    }
}
