package com.jamlapps.meridian.search.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.ProductSearch;
import com.jamlapps.meridian.catalog.ProductSearchCriteria;
import com.jamlapps.meridian.shared.Money;
import java.time.Clock;
import java.util.Currency;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-SCH-01 to 06. See this module's package doc for what BR-SCH-02/03/04 do not cover yet and why. */
@Service
class SearchService {

    private final ProductSearch products;
    private final SearchLogRepository searchLog;
    private final Clock clock;

    SearchService(ProductSearch products, SearchLogRepository searchLog, Clock clock) {
        this.products = products;
        this.searchLog = searchLog;
        this.clock = clock;
    }

    /**
     * BR-SCH-01/02/03/05/06. {@code keyword} substring-matches name and description
     * case-insensitively (BR-SCH-05's "partial matches" — true misspelling tolerance, a
     * Levenshtein- or trigram-based correction, is not built; ILIKE is the honest extent of this
     * pass). A keyword search returning nothing is logged (BR-SCH-06); a filter-only browse with
     * no keyword is not, since there is no search term to review.
     */
    @Transactional
    Page<Product> search(
            @Nullable String keyword,
            @Nullable Long minPriceMinorUnits,
            @Nullable Long maxPriceMinorUnits,
            @Nullable String currency,
            @Nullable UUID vendorId,
            SortOption sort,
            int page,
            int perPage) {
        Currency resolvedCurrency = Currency.getInstance(currency == null ? "KES" : currency);
        Money minPrice = minPriceMinorUnits == null ? null : Money.of(minPriceMinorUnits, resolvedCurrency);
        Money maxPrice = maxPriceMinorUnits == null ? null : Money.of(maxPriceMinorUnits, resolvedCurrency);
        ProductSearchCriteria criteria = new ProductSearchCriteria(normalize(keyword), minPrice, maxPrice, vendorId);

        Page<Product> result = sort == SortOption.RELEVANCE && criteria.keyword() != null
                ? products.searchByRelevance(criteria, PageRequest.of(page, perPage))
                : products.search(criteria, pageableFor(sort, page, perPage));

        if (criteria.keyword() != null && result.isEmpty()) {
            searchLog.save(new SearchLogEntity(UUID.randomUUID(), criteria.keyword(), clock.instant()));
        }

        return result;
    }

    private static Pageable pageableFor(SortOption sort, int page, int perPage) {
        Sort ordering = switch (sort) {
            case PRICE_ASC -> Sort.by("priceMinorUnits").ascending();
            case PRICE_DESC -> Sort.by("priceMinorUnits").descending();
            case RECENCY -> Sort.by("createdAt").descending();
            // RELEVANCE without a keyword has nothing to rank against — recency is the honest fallback.
            case RELEVANCE -> Sort.by("createdAt").descending();
        };
        return PageRequest.of(page, perPage, ordering);
    }

    private static @Nullable String normalize(@Nullable String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
