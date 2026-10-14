package com.jamlapps.meridian.inventory.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * BR-SEC-06, RSK-11: "detect and alert on overselling events, being any confirmed order line
 * exceeding recorded available stock at the point of deduction."
 *
 * <p>BR-INV-04's database constraint ({@code StockRepository.tryReserve}'s atomic, WHERE-guarded
 * UPDATE, backed by {@code stock_reserved_within_total}) already makes a genuine oversold row
 * impossible — {@code reserved_quantity} can never exceed {@code total_quantity}, so there is no
 * data-corruption event for this table to record after the fact. What is real and alert-worthy is
 * the near-miss that constraint exists to produce instead: a reservation attempt refused because
 * demand exceeded what was available, recorded here the moment {@link InventoryService#reserve}
 * sees {@code tryReserve} return zero. This is exactly RSK-11's mitigation intent — an automated
 * signal that reservation logic is under contention — recorded at the point of deduction as the
 * requirement names it, not a stand-in for a corruption event this codebase's own constraint
 * already forecloses.
 */
@Entity
@Table(name = "oversell_attempt")
class OversellAttemptEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "requested_quantity", nullable = false)
    private int requestedQuantity;

    @Column(name = "available_quantity", nullable = false)
    private int availableQuantity;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected OversellAttemptEntity() {
        // for JPA
    }

    OversellAttemptEntity(UUID id, UUID productId, int requestedQuantity, int availableQuantity, UUID referenceId, Instant occurredAt) {
        this.id = id;
        this.productId = productId;
        this.requestedQuantity = requestedQuantity;
        this.availableQuantity = availableQuantity;
        this.referenceId = referenceId;
        this.occurredAt = occurredAt;
    }

    OversellAttemptView toView() {
        return new OversellAttemptView(id, productId, requestedQuantity, availableQuantity, referenceId, occurredAt);
    }
}
