package com.jamlapps.meridian.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * BR-SCH-01/03/04: the query primitive {@code search} builds on. Always restricted to ACTIVE
 * products — BR-SCH-04's "exclude out-of-stock, delisted, and suspended-vendor products" holds
 * for delisted and suspended-vendor for free here (a suspended vendor's products are already
 * DELISTED by {@code VendorSuspensionListener} before this is ever queried); out-of-stock is not
 * excluded — that needs {@code inventory}, which this interface's caller does not depend on. See
 * {@code search}'s package doc.
 */
public interface ProductSearch {

    Page<Product> search(ProductSearchCriteria criteria, Pageable pageable);

    /**
     * BR-SCH-03's relevance sort. Requires {@code criteria.keyword()} — relevance has nothing to
     * rank against without one — and {@code pageable} carrying no {@code Sort} of its own; the
     * ranking itself is this method's job; see the underlying query's javadoc for why supplying
     * one would not do what a caller might expect.
     */
    Page<Product> searchByRelevance(ProductSearchCriteria criteria, Pageable pageable);
}
