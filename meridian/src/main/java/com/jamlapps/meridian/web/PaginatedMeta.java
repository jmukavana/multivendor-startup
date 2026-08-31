package com.jamlapps.meridian.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Metadata for a collection response.
 *
 * <p>{@code appliedFilters} echoes the filters the server actually honoured, which is not always
 * what was asked for — an unknown parameter is ignored rather than failing the request, and the
 * frontend needs to know which of its filters took effect.
 */
public record PaginatedMeta(
        String traceId,
        Instant generatedAt,
        Pagination pagination,
        List<SortSpec> sort,
        Map<String, String> appliedFilters)
        implements ApiMeta {

    public PaginatedMeta {
        sort = sort == null ? List.of() : List.copyOf(sort);
        appliedFilters = appliedFilters == null ? Map.of() : Map.copyOf(appliedFilters);
    }
}
