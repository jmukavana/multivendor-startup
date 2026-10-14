package com.jamlapps.meridian.order;

import org.jspecify.annotations.Nullable;

/** BR-SEC-03: the checkout request's own device/session characteristics, captured once at order creation. */
public record RequestOrigin(@Nullable String sourceAddress, @Nullable String userAgent) {

    public static final RequestOrigin NONE = new RequestOrigin(null, null);
}
