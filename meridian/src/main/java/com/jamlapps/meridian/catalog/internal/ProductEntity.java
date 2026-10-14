package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.ProductStatus;
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
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "product")
class ProductEntity {

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(nullable = false)
    private String name;

    private @Nullable String description;

    @Column(name = "price_minor_units", nullable = false)
    private long priceMinorUnits;

    @Column(nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {
        // for JPA
    }

    ProductEntity(UUID id, UUID vendorId, String name, @Nullable String description, Money price, Instant now) {
        this.id = id;
        this.vendorId = vendorId;
        this.name = name;
        this.description = description;
        this.priceMinorUnits = price.minorUnits();
        this.currency = price.currency().getCurrencyCode();
        // BR-CAT-06/AC-CAT-02: created Draft, not Active — see ProductStatus's javadoc.
        this.status = ProductStatus.DRAFT;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** BR-CAT-06: DRAFT -> ACTIVE. Preconditions (image present, title clean) are the service's job. */
    void publish(Instant now) {
        this.status = ProductStatus.ACTIVE;
        this.updatedAt = now;
    }

    void delist(Instant now) {
        this.status = ProductStatus.DELISTED;
        this.updatedAt = now;
    }

    /** @return the price before the change, for the caller to record as price history (BR-CAT-09) */
    Money changePrice(Money newPrice, Instant now) {
        Money previous = Money.of(priceMinorUnits, Currency.getInstance(currency));
        this.priceMinorUnits = newPrice.minorUnits();
        this.currency = newPrice.currency().getCurrencyCode();
        this.updatedAt = now;
        return previous;
    }

    Product toProduct() {
        return new Product(
                id, vendorId, name, description, Money.of(priceMinorUnits, Currency.getInstance(currency)), status, createdAt);
    }

    UUID id() {
        return id;
    }

    UUID vendorId() {
        return vendorId;
    }

    String name() {
        return name;
    }

    ProductStatus status() {
        return status;
    }

    Money price() {
        return Money.of(priceMinorUnits, Currency.getInstance(currency));
    }
}
