package com.jamlapps.meridian.web;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

/**
 * The caller's real address behind a load balancer — factored out of {@code
 * iam.internal.AuthController}, which had this exact logic first, so BR-SEC-01's rate limiting can
 * key on the same address without duplicating it a third time across {@code checkout} and {@code
 * search}.
 */
public final class ClientAddress {

    private ClientAddress() {}

    /**
     * The staging and production topology sits behind a load balancer, so the socket address is
     * the balancer's, not the caller's. X-Forwarded-For carries the real one; its first entry is
     * the original client, per the standard convention.
     */
    public static @Nullable String of(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
