package com.jamlapps.meridian.web.internal;

import com.jamlapps.meridian.web.ErrorCode;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Baseline security posture. Deny by default; open only what has to be open.
 *
 * <p>Authentication is session-token based, per ADR-008. Everything under {@code /api} requires a
 * live session except the endpoints a caller necessarily hits before it has one — registration,
 * one-time code request and verification, and sign-out. Role-level authorisation (BR-IAM-03) is
 * not yet applied per endpoint; every module currently gets "authenticated", not "authenticated as
 * the right role". That is a Phase 2 gap, not an oversight, since no endpoint but {@code auth}
 * exists yet to apply it to.
 *
 * <p>What this does establish is the rejection shape: an unauthenticated or unauthorised request
 * returns the same problem body as any other error, so the frontend's {@code toApiError} reads
 * {@code SESSION_EXPIRED} and {@code FORBIDDEN} rather than falling back to {@code UNKNOWN_ERROR}.
 *
 * <p><strong>The authentication mechanism itself is not referenced here.</strong> {@code iam}
 * contributes a session-resolving filter as a bean named {@code sessionAuthenticationFilter},
 * injected below by that name and typed only as {@code jakarta.servlet.Filter} — a JDK type, not
 * an {@code iam} type. That keeps this class free of any import from {@code iam}, so {@code web}
 * stays the foundational module every other module depends on rather than the reverse. See
 * ADR-008.
 */
@Configuration(proxyBeanMethods = false)
class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(
            HttpSecurity http,
            ProblemFactory problems,
            @Qualifier("sessionAuthenticationFilter") Filter sessionAuthenticationFilter) {
        return http
                // The API is stateless and token-authenticated, so there is no session cookie for
                // an attacker to ride and nothing for CSRF protection to defend. Re-enable the
                // moment any cookie becomes authoritative for authentication.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(sessionAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests
                        // Kubernetes probes these before the pod is in service, so they cannot
                        // require credentials the platform does not have.
                        .requestMatchers("/actuator/health/**", "/actuator/info")
                        .permitAll()
                        // The API document is the contract the frontend generates from. It is
                        // readable inside the cluster and must not be exposed publicly; see the
                        // ingress note in deploy/staging/README.md.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        // BR-IAM-01/02/05: there is necessarily no session yet when a caller is
                        // trying to acquire one. Sign-out is idempotent by design (AuthController
                        // treats a missing cookie as a no-op) so it is safe to permit unauthenticated.
                        .requestMatchers(
                                "/api/auth/register", "/api/auth/code", "/api/auth/verify", "/api/auth/session")
                        .permitAll()
                        // Metrics and the modulith endpoint carry operational detail.
                        .requestMatchers("/actuator/**")
                        .authenticated()
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, exception) -> problems.write(
                                response,
                                HttpStatus.UNAUTHORIZED,
                                ErrorCode.SESSION_EXPIRED,
                                "Your session has expired. Please sign in again."))
                        .accessDeniedHandler((request, response, exception) -> problems.write(
                                response,
                                HttpStatus.FORBIDDEN,
                                ErrorCode.FORBIDDEN,
                                "You do not have permission to do that.")))
                .build();
    }
}
