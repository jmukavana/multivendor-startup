package com.jamlapps.meridian.catalog;

import com.jamlapps.meridian.shared.Money;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A product, as other modules see it.
 *
 * <p><strong>The walking-skeleton shape.</strong> No variants (BR-CAT-04), no category
 * (BR-CAT-03), no images (BR-CAT-05), no price history (BR-CAT-09) — one price, one currency, per
 * product. Phase 2 replaces this with the full BR-CAT-01 to 12 shape; {@code order} depends on
 * {@code catalog}'s public API rather than its entities specifically so that replacement does not
 * ripple outward.
 */
public record Product(
        UUID id,
        UUID vendorId,
        String name,
        @Nullable String description,
        Money price,
        ProductStatus status,
        /** BR-SCH-03: what "recency" sorts by. */
        Instant createdAt) {

    public boolean isOrderable() {
        return status == ProductStatus.ACTIVE;
    }
}
