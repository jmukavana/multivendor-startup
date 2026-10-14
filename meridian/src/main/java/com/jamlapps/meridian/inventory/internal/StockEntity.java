package com.jamlapps.meridian.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock")
class StockEntity {

    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "total_quantity", nullable = false)
    private int totalQuantity;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StockEntity() {
        // for JPA
    }

    StockEntity(UUID productId, int totalQuantity, Instant updatedAt) {
        this.productId = productId;
        this.totalQuantity = totalQuantity;
        this.reservedQuantity = 0;
        this.updatedAt = updatedAt;
    }

    int available() {
        return totalQuantity - reservedQuantity;
    }

    UUID productId() {
        return productId;
    }

    int totalQuantity() {
        return totalQuantity;
    }

    int reservedQuantity() {
        return reservedQuantity;
    }
}
