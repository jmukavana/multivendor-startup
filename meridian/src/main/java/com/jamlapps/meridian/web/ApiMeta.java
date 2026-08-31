package com.jamlapps.meridian.web;

import java.time.Instant;

/**
 * Metadata accompanying every successful response.
 *
 * <p>Mirrors {@code ApiMeta} in {@code meridian-frontend/src/shared/api/types.ts}.
 */
public sealed interface ApiMeta permits ResponseMeta, PaginatedMeta {

    /**
     * Correlates this response with its server-side trace. The frontend surfaces it on error
     * screens so a buyer-reported failure can be traced to the module that produced it.
     */
    String traceId();

    Instant generatedAt();
}
