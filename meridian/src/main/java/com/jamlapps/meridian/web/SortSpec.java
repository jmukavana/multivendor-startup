package com.jamlapps.meridian.web;

/** One applied sort, echoed back so the frontend can render the active ordering. */
public record SortSpec(String field, String direction) {

    public static SortSpec asc(String field) {
        return new SortSpec(field, "asc");
    }

    public static SortSpec desc(String field) {
        return new SortSpec(field, "desc");
    }
}
