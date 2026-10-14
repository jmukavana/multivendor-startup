package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.Role;
import com.jamlapps.meridian.iam.internal.AuthenticationService.IssuedSession;
import com.jamlapps.meridian.iam.internal.AuthenticationService.RequestContext;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.ClientAddress;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-IAM-01, BR-IAM-02, BR-IAM-05: registration, one-time codes, sessions.
 *
 * <p><strong>The token travels in the response body, not a {@code Set-Cookie} header.</strong>
 * {@code meridian-frontend/src/shared/api/client.ts} is {@code "server-only"} — the browser never
 * calls this API directly, only the Next.js server does, over a plain server-to-server
 * {@code fetch}. A {@code Set-Cookie} header on that response does not reach the browser; Node's
 * {@code fetch} does not forward it. The Next.js server must read {@code data.token} from this
 * response and set the {@code meridian_session} cookie on its own response to the browser, with
 * {@code cookies().set(...)} in a Server Action, exactly as {@code shared/lib/session.ts} reads it
 * back with {@code cookies().get(...)}. Sending a browser-facing cookie from here would be dead
 * code that looks load-bearing.
 *
 * <p>Every authenticated call — from this API's point of view — carries {@code Authorization:
 * Bearer <token>}, resolved by {@code SessionAuthenticationFilter}. That is what the frontend's
 * {@code apiFetch} already sends, and it is what {@link #signOut} reads directly, for the same
 * reason: there is no browser cookie to read here.
 */
@RestController
@RequestMapping("/auth")
class AuthController {

    private final AuthenticationService authentication;

    AuthController(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    record RegisterRequest(
            @NotBlank(message = "must not be blank") String identifier,
            @NotBlank(message = "must not be blank") String displayName) {}

    record IdentifierRequest(@NotBlank(message = "must not be blank") String identifier) {}

    record VerifyRequest(
            @NotBlank(message = "must not be blank") String identifier,
            @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code) {}

    /**
     * @param token the session token. The frontend's Server Action stores this in the
     *     {@code meridian_session} cookie and never anywhere else — see the class doc.
     */
    record SessionResponse(String token, Instant expiresAt, String accountId, Set<Role> roles, boolean canCheckOut) {}

    @PostMapping("/register")
    ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        authentication.register(request.identifier(), request.displayName(), contextOf(httpRequest));
        return ApiResponse.of(null);
    }

    @PostMapping("/code")
    ApiResponse<Void> requestCode(@Valid @RequestBody IdentifierRequest request, HttpServletRequest httpRequest) {
        authentication.requestCode(request.identifier(), contextOf(httpRequest));
        return ApiResponse.of(null);
    }

    @PostMapping("/verify")
    ApiResponse<SessionResponse> verify(@Valid @RequestBody VerifyRequest request, HttpServletRequest httpRequest) {
        IssuedSession issued =
                authentication.verifyCode(request.identifier(), request.code(), contextOf(httpRequest));

        return ApiResponse.of(new SessionResponse(
                issued.token(),
                issued.expiresAt(),
                issued.account().id().toString(),
                issued.account().roles(),
                issued.account().canCheckOut()));
    }

    /** Signs out the calling session. Requires the same bearer token every other call uses. */
    @DeleteMapping("/session")
    ApiResponse<Void> signOut(HttpServletRequest httpRequest) {
        String token = bearerToken(httpRequest);

        if (token != null) {
            authentication.revoke(token, contextOf(httpRequest));
        }

        return ApiResponse.of(null);
    }

    @DeleteMapping("/sessions")
    ApiResponse<Void> signOutEverywhere(
            @AuthenticationPrincipal AuthenticatedCaller principal, HttpServletRequest httpRequest) {

        authentication.revokeAllSessions(principal.accountId(), contextOf(httpRequest));
        return ApiResponse.of(null);
    }

    private static @Nullable String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            return null;
        }
        String token = header.substring("Bearer ".length()).trim();
        return token.isEmpty() ? null : token;
    }

    private static RequestContext contextOf(HttpServletRequest request) {
        return new RequestContext(ClientAddress.of(request), request.getHeader("User-Agent"));
    }
}
