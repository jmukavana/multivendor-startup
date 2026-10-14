package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryRateCard;
import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

/** BR-FUL-08, DAT-07: versioned, matching {@code pricing.internal.CommissionConfigurationEntity} exactly — never updated in place. */
@Entity
@Table(name = "delivery_rate_card")
class DeliveryRateCardEntity {

    @Id
    private UUID id;

    @Column(name = "carrier_id", nullable = false)
    private UUID carrierId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryMethod method;

    @Column(name = "price_minor_units", nullable = false)
    private long priceMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeliveryRateCardEntity() {
        // for JPA
    }

    DeliveryRateCardEntity(UUID id, UUID carrierId, UUID zoneId, DeliveryMethod method, Money price, Instant effectiveFrom, Instant createdAt) {
        this.id = id;
        this.carrierId = carrierId;
        this.zoneId = zoneId;
        this.method = method;
        this.priceMinorUnits = price.minorUnits();
        this.currency = price.currency().getCurrencyCode();
        this.effectiveFrom = effectiveFrom;
        this.createdAt = createdAt;
    }

    Money price() {
        return Money.of(priceMinorUnits, Currency.getInstance(currency));
    }

    DeliveryRateCard toDeliveryRateCard() {
        return new DeliveryRateCard(id, carrierId, zoneId, method, price(), effectiveFrom);
    }
}
