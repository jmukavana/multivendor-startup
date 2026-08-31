package com.jamlapps.meridian.web;

import java.time.Instant;

/** Metadata for a single-resource response. */
public record ResponseMeta(String traceId, Instant generatedAt) implements ApiMeta {}
