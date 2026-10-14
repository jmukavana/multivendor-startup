package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.catalog.Products;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-INV-01: setting how much of a product exists. Not described by any BR- requirement as a named
 * endpoint, but stock has to enter the system somewhere for BR-INV-02's reservation to have
 * anything to reserve against — the owning vendor is the only party who could know the number.
 */
@Service
class StockManagementService {

    private final StockRepository stock;
    private final StockMovementRepository movements;
    private final Products products;
    private final Vendors vendors;
    private final Clock clock;

    StockManagementService(
            StockRepository stock,
            StockMovementRepository movements,
            Products products,
            Vendors vendors,
            Clock clock) {
        this.stock = stock;
        this.movements = movements;
        this.products = products;
        this.vendors = vendors;
        this.clock = clock;
    }

    /**
     * Sets the total quantity on hand, up or down, recording the delta as a RESTOCK movement.
     * Ownership is checked against the product's vendor, not against a stock row the caller might
     * not know exists yet — a product with no stock row at all is exactly the case this method
     * exists to handle first.
     */
    @Transactional
    void setTotalQuantity(UUID productId, UUID callerAccountId, int newTotal) {
        if (newTotal < 0) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Stock quantity must not be negative.");
        }

        var product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product", productId));
        var vendor = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may manage stock."));

        if (!product.vendorId().equals(vendor.id())) {
            // Same disclosure reasoning as catalog.CatalogService.requireOwned: another vendor's
            // product 404s rather than confirming it exists under someone else's ownership.
            throw ApiException.notFound("Product", productId);
        }

        Instant now = clock.instant();
        int previousTotal = stock.findByProductId(productId).map(StockEntity::totalQuantity).orElse(0);
        int delta = newTotal - previousTotal;

        stock.findByProductId(productId).ifPresentOrElse(
                existing -> {
                    // A restock while reservations are outstanding must not let total drop below
                    // what is already reserved — that would violate stock_reserved_within_total,
                    // the invariant the schema itself enforces as a last line of defence.
                    if (newTotal < existing.reservedQuantity()) {
                        throw new ApiException(
                                ErrorCode.VALIDATION_ERROR,
                                "Cannot set total quantity below the " + existing.reservedQuantity()
                                        + " units currently reserved.");
                    }
                },
                () -> {});

        if (delta != 0) {
            recordDeltaAndUpsert(productId, newTotal, now);
            movements.save(new StockMovementEntity(
                    UUID.randomUUID(), productId, delta, StockMovementEntity.Reason.RESTOCK, null, now));
        }
    }

    @Transactional(readOnly = true)
    int totalQuantity(UUID productId) {
        return stock.findByProductId(productId).map(StockEntity::totalQuantity).orElse(0);
    }

    @Transactional(readOnly = true)
    int availableQuantity(UUID productId) {
        return stock.findByProductId(productId).map(StockEntity::available).orElse(0);
    }

    @Transactional(readOnly = true)
    List<StockMovementEntity> movementHistory(UUID productId, UUID callerAccountId) {
        var product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product", productId));
        var vendor = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may view stock movements."));

        if (!product.vendorId().equals(vendor.id())) {
            throw ApiException.notFound("Product", productId);
        }

        return movements.findByProductIdOrderByOccurredAtDesc(productId);
    }

    private void recordDeltaAndUpsert(UUID productId, int newTotal, Instant now) {
        if (stock.findByProductId(productId).isEmpty()) {
            stock.save(new StockEntity(productId, newTotal, now));
        } else {
            // A direct update rather than loading, mutating and saving the entity: setting the
            // total is the one stock mutation that is not part of the reserve/release/confirm
            // concurrency-sensitive trio, but going through JPA's normal save path here would
            // still race a concurrent reserve() reading a stale reservedQuantity between this
            // read and that write. An explicit UPDATE avoids it the same way tryReserve does.
            stock.setTotalQuantity(productId, newTotal, now);
        }
    }
}
