package com.jamlapps.meridian.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-INV-08, AC-INV-04: append-only, matching this codebase's other audit-log tables. */
@Entity
@Table(name = "stock_movement")
class StockMovementEntity {

    /**
     * What {@code quantityDelta} means depends on which of these it is paired with — the ledger
     * tracks two different physical facts and both belong in one table (BR-INV-08 asks for "every
     * stock movement", not two separate ledgers):
     *
     * <ul>
     *   <li>{@code RESTOCK}, {@code CONFIRMED}, {@code CANCELLED}: total stock physically on hand
     *       changed.
     *   <li>{@code RESERVED}, {@code RELEASED}, {@code EXPIRED}: nothing physical changed; what is
     *       available to buyers did, because a reservation moved quantity into or out of the
     *       reserved bucket. Confirming a reservation does not appear here as an availability
     *       change — that quantity was already excluded from availability the moment it was
     *       reserved.
     * </ul>
     *
     * <p>{@code CANCELLED} (BR-ORD-13/14) is the inverse of {@code CONFIRMED}: a buyer cancelled a
     * sub-order whose reservation had already been confirmed (stock genuinely deducted), so the
     * physical stock is given back — distinct from {@code RELEASED}, which only ever un-does a
     * reservation that was never confirmed in the first place.
     */
    enum Reason {
        RESTOCK,
        RESERVED,
        RELEASED,
        CONFIRMED,
        EXPIRED,
        CANCELLED
    }

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Reason reason;

    @Column(name = "reference_id")
    private @Nullable UUID referenceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected StockMovementEntity() {
        // for JPA
    }

    StockMovementEntity(
            UUID id, UUID productId, int quantityDelta, Reason reason, @Nullable UUID referenceId, Instant occurredAt) {
        this.id = id;
        this.productId = productId;
        this.quantityDelta = quantityDelta;
        this.reason = reason;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    int quantityDelta() {
        return quantityDelta;
    }

    Reason reason() {
        return reason;
    }

    @Nullable UUID referenceId() {
        return referenceId;
    }

    Instant occurredAt() {
        return occurredAt;
    }
}
