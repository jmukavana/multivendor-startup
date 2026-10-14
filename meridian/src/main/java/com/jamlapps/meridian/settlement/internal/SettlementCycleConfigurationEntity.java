package com.jamlapps.meridian.settlement.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Immutable once created — see {@code settlement.SettlementCycle}'s javadoc. */
@Entity
@Table(name = "settlement_cycle_configuration")
class SettlementCycleConfigurationEntity {

    @Id
    private UUID id;

    @Column(name = "cycle_days", nullable = false)
    private int cycleDays;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected SettlementCycleConfigurationEntity() {
        // for JPA
    }

    SettlementCycleConfigurationEntity(UUID id, int cycleDays, Instant effectiveFrom, Instant createdAt) {
        this.id = id;
        this.cycleDays = cycleDays;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    int cycleDays() {
        return cycleDays;
    }
}
