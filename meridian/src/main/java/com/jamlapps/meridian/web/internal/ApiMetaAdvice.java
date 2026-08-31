package com.jamlapps.meridian.web.internal;

import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.PaginatedMeta;
import com.jamlapps.meridian.web.ResponseMeta;
import io.micrometer.tracing.Tracer;
import java.time.Clock;
import java.time.Instant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

/**
 * Stamps {@code traceId} and {@code generatedAt} onto every {@link ApiResponse} as it is written.
 *
 * <p>Controllers therefore never construct metadata, which removes the two ways it would otherwise
 * go wrong: forgetting it on a new endpoint, and formatting the timestamp differently in each one.
 */
@RestControllerAdvice
class ApiMetaAdvice implements ResponseBodyAdvice<ApiResponse<?>> {

    private final Tracer tracer;
    private final Clock clock;

    ApiMetaAdvice(Tracer tracer, Clock clock) {
        this.tracer = tracer;
        this.clock = clock;
    }

    @Override
    public boolean supports(MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return ApiResponse.class.isAssignableFrom(returnType.getParameterType())
                || ApiResponse.class.isAssignableFrom(resolveBodyType(returnType));
    }

    @Override
    public @Nullable ApiResponse<?> beforeBodyWrite(
            @Nullable ApiResponse<?> body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        Instant now = clock.instant();
        String traceId = currentTraceId();

        // A paged response already carries pagination that must survive; only the two stamped
        // fields are replaced.
        if (body.meta() instanceof PaginatedMeta paged) {
            return new ApiResponse<>(
                    body.data(),
                    new PaginatedMeta(traceId, now, paged.pagination(), paged.sort(), paged.appliedFilters()));
        }

        return new ApiResponse<>(body.data(), new ResponseMeta(traceId, now));
    }

    /**
     * Returns the active trace id, or null when nothing is tracing. Sampling can be off and the
     * application must still serve responses, so the absence of a trace is not an error — the
     * field is simply omitted, which the frontend already treats as optional.
     */
    private @Nullable String currentTraceId() {
        var span = tracer.currentSpan();
        return span == null ? null : span.context().traceId();
    }

    /** Unwraps ResponseEntity&lt;ApiResponse&lt;T&gt;&gt; so those endpoints are stamped too. */
    private static Class<?> resolveBodyType(MethodParameter returnType) {
        return returnType.nested().getNestedParameterType();
    }
}
