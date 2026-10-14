package com.jamlapps.meridian.catalog;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * BR-SCH-01/02: every filter is optional — {@code null} means "no constraint on this dimension",
 * not "match nothing". {@code keyword} matches BR-SCH-01's "title, description, and attributes"
 * as a case-insensitive substring against name and description; there are no separate attributes
 * yet (BR-CAT-04's variants, not built). Category and rating filters from BR-SCH-02 are not here —
 * see {@code search}'s own package doc on why.
 */
public record ProductSearchCriteria(@Nullable String keyword, @Nullable Money minPrice, @Nullable Money maxPrice, @Nullable UUID vendorId) {}
