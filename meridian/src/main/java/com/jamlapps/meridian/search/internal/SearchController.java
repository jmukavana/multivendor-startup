package com.jamlapps.meridian.search.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.ClientAddress;
import com.jamlapps.meridian.web.ErrorCode;
import com.jamlapps.meridian.web.RateLimiter;
import com.jamlapps.meridian.web.SortSpec;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** BR-SCH-01 to 06. BR-IAM-08-style guest access — no authentication required, matching {@code catalog}'s own product listing. */
@RestController
@RequestMapping("/search/products")
class SearchController {

    private final SearchService service;
    private final RateLimiter rateLimiter;

    SearchController(SearchService service, RateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping
    ApiResponse<List<Product>> search(
            @RequestParam(required = false) @Nullable String keyword,
            @RequestParam(required = false) @Nullable Long minPriceMinorUnits,
            @RequestParam(required = false) @Nullable Long maxPriceMinorUnits,
            @RequestParam(required = false) @Nullable String currency,
            @RequestParam(required = false) @Nullable UUID vendorId,
            @RequestParam(defaultValue = "RELEVANCE") SortOption sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage,
            HttpServletRequest httpRequest) {

        // BR-SEC-01: "search" — by source address only. Guest browsing (BR-IAM-08) means there is
        // often no account to key on; a source address is the one identity every caller has.
        String sourceAddress = ClientAddress.of(httpRequest);
        if (sourceAddress != null && !rateLimiter.tryConsume(RateLimiter.SCOPE_SEARCH_SOURCE, sourceAddress)) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Too many search requests. Try again shortly.");
        }

        Page<Product> result =
                service.search(keyword, minPriceMinorUnits, maxPriceMinorUnits, currency, vendorId, sort, page, perPage);

        return ApiResponse.paged(
                result.getContent(),
                page,
                perPage,
                result.getTotalElements(),
                List.of(SortSpec.desc(sort.name().toLowerCase())),
                Map.of());
    }
}
