package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.OrderLine;
import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "order_line")
class OrderLineEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "sub_order_id", nullable = false)
    private SubOrderEntity subOrder;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;

    @Column(name = "unit_price_minor_units", nullable = false)
    private long unitPriceMinorUnits;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "line_total_minor_units", nullable = false)
    private long lineTotalMinorUnits;

    @Column(nullable = false)
    private String currency;

    protected OrderLineEntity() {
        // for JPA
    }

    OrderLineEntity(
            UUID id, SubOrderEntity subOrder, UUID productId, String productNameSnapshot, Money unitPrice, int quantity) {
        this.id = id;
        this.subOrder = subOrder;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.unitPriceMinorUnits = unitPrice.minorUnits();
        this.quantity = quantity;
        this.lineTotalMinorUnits = unitPrice.times(quantity).minorUnits();
        this.currency = unitPrice.currency().getCurrencyCode();
    }

    Money lineTotal() {
        return Money.of(lineTotalMinorUnits, Currency.getInstance(currency));
    }

    OrderLine toOrderLine() {
        Currency curr = Currency.getInstance(currency);
        return new OrderLine(
                productId,
                productNameSnapshot,
                Money.of(unitPriceMinorUnits, curr),
                quantity,
                Money.of(lineTotalMinorUnits, curr));
    }
}
