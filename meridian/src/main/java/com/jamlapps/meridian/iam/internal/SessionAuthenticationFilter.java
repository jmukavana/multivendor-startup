package com.jamlapps.meridian.iam.internal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Resolves {@code Authorization: Bearer <token>} into an authentication, per ADR-008.
 *
 * <p>A rejected token leaves the context empty rather than throwing. The filter's job is to
 * establish identity where it can; deciding what an anonymous caller may do belongs to the
 * authorisation rules, and a filter that throws would bypass them for endpoints that permit
 * anonymous access.
 *
 * <p>Deliberately has no {@code @Transactional} anywhere in this class — see {@link
 * SessionResolver}'s javadoc for why that matters for a class in this hierarchy specifically.
 */
@Component
class SessionAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final SessionResolver sessionResolver;

    SessionAuthenticationFilter(SessionResolver sessionResolver) {
        this.sessionResolver = sessionResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String token = extractToken(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            sessionResolver.resolve(token).ifPresent(caller -> {
                var authorities = caller.authorities().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // Credentials are null on purpose: the token must not be reachable from the
                // security context, where anything that logs the authentication would print it.
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(caller, null, authorities));
            });
        }

        chain.doFilter(request, response);
    }

    private static @Nullable String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
