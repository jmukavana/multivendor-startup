package com.jamlapps.meridian.web;

import java.util.List;
import java.util.Map;

/**
 * The success envelope returned by every endpoint: {@code { "data": ..., "meta": ... }}.
 *
 * <p>Controllers construct it without metadata — {@code ApiResponse.of(product)}. The trace id and
 * timestamp are stamped on the way out by {@code ApiMetaAdvice}, so no controller has to remember
 * to populate them and none can populate them inconsistently.
 *
 * @param data the payload; a single object, or a list for collection endpoints
 * @param meta populated during response writing, and null until then
 */
public record ApiResponse<T>(T data, ApiMeta meta) {

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, null);
    }

    /**
     * A collection response carrying page position, applied sorts and honoured filters.
     *
     * @param zeroBasedPage the page index as Spring Data reports it; converted to 1-based here
     */
    public static <T> ApiResponse<List<T>> paged(
            List<T> items,
            int zeroBasedPage,
            int perPage,
            long totalItems,
            List<SortSpec> sort,
            Map<String, String> appliedFilters) {

        return new ApiResponse<>(
                List.copyOf(items),
                new PaginatedMeta(
                        null, null, Pagination.of(zeroBasedPage, perPage, totalItems), sort, appliedFilters));
    }
}
