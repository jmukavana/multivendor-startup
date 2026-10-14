package com.jamlapps.meridian.catalog;

import java.util.Optional;
import java.util.UUID;

/** How other modules ask about products. Read-only, matching the pattern in {@code iam} and {@code vendor}. */
public interface Products {

    Optional<Product> findById(UUID productId);
}
