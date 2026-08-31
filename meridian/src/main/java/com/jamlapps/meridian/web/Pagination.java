package com.jamlapps.meridian.web;

/**
 * Page position for a collection response.
 *
 * <p>{@code page} is 1-based, matching what the frontend renders. Spring Data is 0-based, so
 * conversion happens here rather than leaking a repository detail into the API.
 *
 * <p>{@code from} and {@code to} are 1-based inclusive item positions, for "showing 21-40 of 137".
 * They are computed rather than supplied so they cannot disagree with the rest.
 */
public record Pagination(
        int page, int perPage, long totalItems, int totalPages, long from, long to) {

    public static Pagination of(int zeroBasedPage, int perPage, long totalItems) {
        if (perPage <= 0) {
            throw new IllegalArgumentException("perPage must be positive, was " + perPage);
        }

        int page = zeroBasedPage + 1;
        int totalPages = (int) Math.ceil((double) totalItems / perPage);
        long offset = (long) zeroBasedPage * perPage;

        // An empty page reports 0..0 rather than 1..0, so the frontend has no negative range to
        // special-case when a filter matches nothing.
        long itemsOnPage = Math.max(0, Math.min(perPage, totalItems - offset));
        long from = itemsOnPage == 0 ? 0 : offset + 1;
        long to = itemsOnPage == 0 ? 0 : offset + itemsOnPage;

        return new Pagination(page, perPage, totalItems, totalPages, from, to);
    }
}
