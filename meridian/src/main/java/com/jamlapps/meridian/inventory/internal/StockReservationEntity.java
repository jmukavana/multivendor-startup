package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.inventory.Reservation;
import com.jamlapps.meridian.inventory.ReservationOutcome;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "stock_reservation")
class StockReservationEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationOutcome status;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private @Nullable Instant resolvedAt;

    protected StockReservationEntity() {
        // for JPA
    }

    StockReservationEntity(UUID id, UUID productId, int quantity, UUID referenceId, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.quantity = quantity;
        this.status = ReservationOutcome.RESERVED;
        this.referenceId = referenceId;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    boolean isLive() {
        return status == ReservationOutcome.RESERVED;
    }

    ReservationOutcome status() {
        return status;
    }

    void resolve(ReservationOutcome outcome, Instant now) {
        this.status = outcome;
        this.resolvedAt = now;
    }

    Reservation toReservation() {
        return new Reservation(id, productId, quantity, status, referenceId, expiresAt);
    }

    UUID id() {
        return id;
    }

    UUID productId() {
        return productId;
    }

    int quantity() {
        return quantity;
    }

    UUID referenceId() {
        return referenceId;
    }

    Instant expiresAt() {
        return expiresAt;
    }
}
