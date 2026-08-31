package com.jamlapps.meridian.web;

import org.springframework.http.HttpStatus;

/**
 * Machine-readable error codes. The frontend branches on these, never on the message.
 *
 * <p>Every constant here must exist in {@code API_ERROR_CODES} in
 * {@code meridian-frontend/src/shared/api/error.ts}. A code the frontend does not know falls
 * through to its generic copy, which is a degraded experience rather than a failure — but the two
 * lists are a contract and should be changed together.
 */
public enum ErrorCode {

    /** No credentials, or credentials that are not valid. */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED),

    /** Authenticated, but not permitted. Distinct from UNAUTHORIZED: re-authenticating will not help. */
    FORBIDDEN(HttpStatus.FORBIDDEN),

    /** A valid session that has expired. The frontend re-authenticates on this and not on UNAUTHORIZED. */
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED),

    NOT_FOUND(HttpStatus.NOT_FOUND),

    /** Request shape or field-level validation failure. Carries fieldErrors. */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    /** A vendor application quoting a KRA PIN already held by another vendor (BR-VND-02). */
    KRA_PIN_TAKEN(HttpStatus.CONFLICT),

    /** Anything unhandled. The detail is deliberately generic; the trace id carries the specifics. */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus defaultStatus;

    ErrorCode(HttpStatus defaultStatus) {
        this.defaultStatus = defaultStatus;
    }

    public HttpStatus defaultStatus() {
        return defaultStatus;
    }
}
