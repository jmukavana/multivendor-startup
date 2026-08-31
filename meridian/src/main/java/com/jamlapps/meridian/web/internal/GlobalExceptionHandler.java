package com.jamlapps.meridian.web.internal;

import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Translates exceptions into the problem shape the frontend already parses.
 *
 * <p>The emitted body carries {@code title}, {@code status}, {@code detail}, {@code code},
 * {@code traceId}, {@code timestamp} and, on validation failures, {@code fieldErrors} — matching
 * {@code ApiProblem} and {@code toApiError} in {@code meridian-frontend/src/shared/api/error.ts}.
 *
 * <p>The rule for messages: a client sees a message only when the code path deliberately produced
 * one. Anything unhandled returns generic copy and puts the specifics in the log against the trace
 * id, because an unhandled exception's message is written for an operator and can disclose schema,
 * file paths or provider responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ProblemFactory problems;

    GlobalExceptionHandler(ProblemFactory problems) {
        this.problems = problems;
    }

    @ExceptionHandler(ApiException.class)
    ProblemDetail handleApiException(ApiException exception) {
        // Deliberate, client-facing, and already carries its own code — logged at debug because a
        // 404 or a validation failure is normal traffic, not an incident.
        log.debug("Handled API exception: {}", exception.getMessage());
        return problem(exception.status(), exception.code(), exception.getMessage(), exception.fieldErrors());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleBeanValidation(MethodArgumentNotValidException exception) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

        for (var error : exception.getBindingResult().getFieldErrors()) {
            String message = error.getDefaultMessage();
            fieldErrors
                    .computeIfAbsent(error.getField(), key -> new ArrayList<>())
                    .add(message == null ? "is invalid" : message);
        }

        // Object-level errors have no field to attach to; they would otherwise vanish silently.
        for (var error : exception.getBindingResult().getGlobalErrors()) {
            String message = error.getDefaultMessage();
            fieldErrors
                    .computeIfAbsent("_", key -> new ArrayList<>())
                    .add(message == null ? "is invalid" : message);
        }

        return problem(
                HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "The request failed validation", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException exception) {
        Map<String, List<String>> fieldErrors = new LinkedHashMap<>();

        for (var violation : exception.getConstraintViolations()) {
            String path = violation.getPropertyPath().toString();
            // Method parameter paths arrive as "method.argument"; the client only knows the last part.
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            fieldErrors.computeIfAbsent(field, key -> new ArrayList<>()).add(violation.getMessage());
        }

        return problem(
                HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "The request failed validation", fieldErrors);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthentication(AuthenticationException exception) {
        log.debug("Authentication failed: {}", exception.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED, "Authentication is required", Map.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException exception) {
        // Distinct from UNAUTHORIZED on purpose: the frontend re-authenticates on one and not the
        // other, and telling an authenticated user to log in again for a permission problem is a
        // loop they cannot escape.
        log.debug("Access denied: {}", exception.getMessage());
        return problem(
                HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "You do not have permission to do that", Map.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail handleNoResource(NoResourceFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "No such endpoint", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        // The only place a stack trace is logged at error level. The trace id in the response is
        // what connects the buyer's screenshot to this line.
        log.error("Unhandled exception", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Something went wrong. Quote the trace id if you report this.",
                Map.of());
    }

    private ProblemDetail problem(
            HttpStatus status, ErrorCode code, String detail, Map<String, List<String>> fieldErrors) {
        return problems.create(status, code, detail, fieldErrors);
    }
}
