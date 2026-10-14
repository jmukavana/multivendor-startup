package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.ProductSearch;
import com.jamlapps.meridian.catalog.ProductSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class ProductSearchAdapter implements ProductSearch {

    private final ProductRepository repository;

    ProductSearchAdapter(ProductRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> search(ProductSearchCriteria criteria, Pageable pageable) {
        return repository
                .search(criteria.keyword(), minPriceMinorUnits(criteria), maxPriceMinorUnits(criteria), criteria.vendorId(), pageable)
                .map(ProductEntity::toProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Product> searchByRelevance(ProductSearchCriteria criteria, Pageable pageable) {
        if (criteria.keyword() == null) {
            throw new IllegalArgumentException("Relevance sort requires a keyword.");
        }

        return repository
                .searchByRelevance(
                        criteria.keyword(), minPriceMinorUnits(criteria), maxPriceMinorUnits(criteria), criteria.vendorId(), pageable)
                .map(ProductEntity::toProduct);
    }

    private static Long minPriceMinorUnits(ProductSearchCriteria criteria) {
        return criteria.minPrice() == null ? null : criteria.minPrice().minorUnits();
    }

    private static Long maxPriceMinorUnits(ProductSearchCriteria criteria) {
        return criteria.maxPrice() == null ? null : criteria.maxPrice().minorUnits();
    }
}
