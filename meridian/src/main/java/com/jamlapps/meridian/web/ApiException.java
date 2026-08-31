package com.jamlapps.meridian.web;

import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * An error a client is meant to see, carrying a code the frontend can branch on.
 *
 * <p>Thrown by any module. Anything else that escapes a controller becomes an INTERNAL_ERROR with a
 * generic message, on the principle that an unhandled exception's message is written for an
 * operator and may disclose more than a client should see.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;
    private final transient Map<String, List<String>> fieldErrors;

    public ApiException(ErrorCode code, String detail) {
        this(code, detail, code.defaultStatus(), Map.of());
    }

    public ApiException(ErrorCode code, String detail, HttpStatus status, Map<String, List<String>> fieldErrors) {
        super(detail);
        this.code = code;
        this.status = status;
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public static ApiException notFound(String what, Object id) {
        return new ApiException(ErrorCode.NOT_FOUND, what + " " + id + " was not found");
    }

    public static ApiException validation(String detail, Map<String, List<String>> fieldErrors) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, detail, HttpStatus.BAD_REQUEST, fieldErrors);
    }

    public ErrorCode code() {
        return code;
    }

    public HttpStatus status() {
        return status;
    }

    public Map<String, List<String>> fieldErrors() {
        return fieldErrors;
    }
}
