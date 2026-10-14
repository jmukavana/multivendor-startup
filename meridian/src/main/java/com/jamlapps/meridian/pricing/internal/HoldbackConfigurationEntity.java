package com.jamlapps.meridian.pricing.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Immutable once referenced by a sub-order — see {@code PricingConfiguration}'s javadoc. */
@Entity
@Table(name = "holdback_configuration")
class HoldbackConfigurationEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private BigDecimal rate;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HoldbackConfigurationEntity() {
        // for JPA
    }

    HoldbackConfigurationEntity(UUID id, BigDecimal rate, Instant effectiveFrom, Instant createdAt) {
        this.id = id;
        this.rate = rate;
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    BigDecimal rate() {
        return rate;
    }
}
