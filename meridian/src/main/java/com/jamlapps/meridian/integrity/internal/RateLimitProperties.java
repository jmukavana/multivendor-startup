package com.jamlapps.meridian.integrity.internal;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * BR-SEC-01/02's "configurable thresholds" — one {@link Rule} per {@code web.RateLimiter.SCOPE_*}
 * constant, overridable under {@code meridian.integrity.rate-limit.*} in {@code
 * application.yaml}; unset fields fall back to the defaults below, the same null-coalescing style
 * as {@code shared.BlobStorageProperties}.
 *
 * <p>Every {@code *Source} rule's default is deliberately far more permissive than its {@code
 * *Identifier}/{@code *Account} counterpart — see {@code web.RateLimiter}'s javadoc for why a
 * source address cannot be held to the same threshold as a phone number or account without
 * punishing every caller behind a shared one, CGNAT above all.
 */
@ConfigurationProperties(prefix = "meridian.integrity.rate-limit")
record RateLimitProperties(
        Rule otcIssuanceInterval,
        Rule otcIssuanceVolumeIdentifier,
        Rule otcIssuanceVolumeSource,
        Rule authVerifyIdentifier,
        Rule authVerifySource,
        Rule checkoutInitiationAccount,
        Rule checkoutInitiationSource,
        Rule searchSource) {

    public RateLimitProperties {
        otcIssuanceInterval = orDefault(otcIssuanceInterval, 1, Duration.ofSeconds(60));
        otcIssuanceVolumeIdentifier = orDefault(otcIssuanceVolumeIdentifier, 5, Duration.ofHours(24));
        otcIssuanceVolumeSource = orDefault(otcIssuanceVolumeSource, 300, Duration.ofHours(1));
        authVerifyIdentifier = orDefault(authVerifyIdentifier, 10, Duration.ofMinutes(15));
        authVerifySource = orDefault(authVerifySource, 200, Duration.ofMinutes(15));
        checkoutInitiationAccount = orDefault(checkoutInitiationAccount, 20, Duration.ofMinutes(15));
        checkoutInitiationSource = orDefault(checkoutInitiationSource, 200, Duration.ofMinutes(15));
        searchSource = orDefault(searchSource, 120, Duration.ofMinutes(1));
    }

    private static Rule orDefault(Rule configured, int maxAttempts, Duration window) {
        return configured != null ? configured : new Rule(maxAttempts, window);
    }

    record Rule(int maxAttempts, Duration window) {}
}
