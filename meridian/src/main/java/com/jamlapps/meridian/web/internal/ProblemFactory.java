package com.jamlapps.meridian.web.internal;

import tools.jackson.databind.ObjectMapper;
import com.jamlapps.meridian.web.ErrorCode;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

/**
 * Builds the problem body, and writes it directly when there is no dispatcher to do it.
 *
 * <p>Two paths produce errors and only one of them reaches a controller advice. Spring Security
 * rejects unauthenticated and unauthorised requests in the filter chain, before
 * {@code DispatcherServlet} runs, so those responses would otherwise carry Spring's default body
 * and the frontend's {@code toApiError} would fall back to "Request failed with status 401" with
 * code {@code UNKNOWN_ERROR}. Both paths build their body here so the shape cannot diverge.
 */
@Component
class ProblemFactory {

    private final Tracer tracer;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    ProblemFactory(Tracer tracer, Clock clock, ObjectMapper objectMapper) {
        this.tracer = tracer;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    ProblemDetail create(HttpStatus status, ErrorCode code, String detail, Map<String, List<String>> fieldErrors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setProperty("code", code.name());
        problem.setProperty("traceId", currentTraceId());
        problem.setProperty("timestamp", clock.instant());

        if (!fieldErrors.isEmpty()) {
            problem.setProperty("fieldErrors", fieldErrors);
        }

        return problem;
    }

    /** Writes a problem to the raw response, for the security filter chain. */
    void write(HttpServletResponse response, HttpStatus status, ErrorCode code, String detail) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), create(status, code, detail, Map.of()));
    }

    private String currentTraceId() {
        var span = tracer.currentSpan();
        // The frontend types traceId as a required string on a problem, so an empty string is sent
        // rather than null when nothing is tracing.
        return span == null ? "" : span.context().traceId();
    }
}
