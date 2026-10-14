package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-CAT-09, AC-CAT-04: append-only, matching this codebase's other audit-log entities. */
@Entity
@Table(name = "product_price_change")
class ProductPriceChangeEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "previous_price_minor_units", nullable = false)
    private long previousPriceMinorUnits;

    @Column(name = "new_price_minor_units", nullable = false)
    private long newPriceMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Column(name = "actor_account_id", nullable = false)
    private UUID actorAccountId;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected ProductPriceChangeEntity() {
        // for JPA
    }

    ProductPriceChangeEntity(
            UUID id, UUID productId, Money previousPrice, Money newPrice, UUID actorAccountId, Instant changedAt) {
        this.id = id;
        this.productId = productId;
        this.previousPriceMinorUnits = previousPrice.minorUnits();
        this.newPriceMinorUnits = newPrice.minorUnits();
        this.currency = newPrice.currency().getCurrencyCode();
        this.actorAccountId = actorAccountId;
        this.changedAt = changedAt;
    }

    PriceChangeView toView() {
        var currencyInstance = java.util.Currency.getInstance(currency);
        return new PriceChangeView(
                Money.of(previousPriceMinorUnits, currencyInstance),
                Money.of(newPriceMinorUnits, currencyInstance),
                actorAccountId,
                changedAt);
    }

    /** AC-CAT-04: previous price, new price, actor and timestamp, retrievable together. */
    record PriceChangeView(Money previousPrice, Money newPrice, UUID actorAccountId, Instant changedAt) {}
}
