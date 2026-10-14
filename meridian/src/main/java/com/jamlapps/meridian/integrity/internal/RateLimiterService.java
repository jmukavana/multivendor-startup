package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.internal.RateLimitProperties.Rule;
import com.jamlapps.meridian.web.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-SEC-01/02: the real implementation behind {@code web.RateLimiter}. Fixed-window, not sliding
 * — a window boundary can let two requests land close together across it, which BR-SEC-01/02 do
 * not require this precisely; a sliding window needs to retain every timestamp in the lookback
 * period rather than one counter per window, which is a real cost this scale does not call for.
 *
 * <p>The bucket increment itself deliberately stays in {@code tryConsume}'s own (usually the
 * caller's) transaction, unlike the breach bookkeeping in {@link RateLimitBreachRecorder} — a
 * refused request's increment rolling back along with its caller's exception is harmless: the
 * count that matters for "is this still over the limit" is whichever value last committed, and
 * that value only changes on an allowed request, which always commits normally. Only the audit
 * trail — a real row that must exist regardless of whether the request that triggered it
 * succeeded — needs to survive a rollback the increment itself does not need to survive.
 */
@Service
class RateLimiterService implements RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    private final RateLimitBucketRepository buckets;
    private final RateLimitBreachRecorder breaches;
    private final RateLimitProperties properties;
    private final Clock clock;

    RateLimiterService(RateLimitBucketRepository buckets, RateLimitBreachRecorder breaches, RateLimitProperties properties, Clock clock) {
        this.buckets = buckets;
        this.breaches = breaches;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    @Transactional
    public boolean tryConsume(String scope, String key) {
        Rule rule = ruleFor(scope);
        Instant now = clock.instant();
        Instant windowStart = flooredTo(now, rule.window());

        buckets.upsertIncrement(UUID.randomUUID(), scope, key, windowStart);
        int count = buckets
                .findByScopeAndKeyAndWindowStart(scope, key, windowStart)
                .map(RateLimitBucketEntity::count)
                .orElse(0);

        boolean allowed = count <= rule.maxAttempts();
        if (!allowed) {
            // REQUIRES_NEW, deliberately not a call on `this` — see RateLimitBreachRecorder's javadoc.
            breaches.record(scope, key, now);
        }
        return allowed;
    }

    private Rule ruleFor(String scope) {
        Rule rule = switch (scope) {
            case SCOPE_OTC_ISSUANCE_INTERVAL -> properties.otcIssuanceInterval();
            case SCOPE_OTC_ISSUANCE_VOLUME_IDENTIFIER -> properties.otcIssuanceVolumeIdentifier();
            case SCOPE_OTC_ISSUANCE_VOLUME_SOURCE -> properties.otcIssuanceVolumeSource();
            case SCOPE_AUTH_VERIFY_IDENTIFIER -> properties.authVerifyIdentifier();
            case SCOPE_AUTH_VERIFY_SOURCE -> properties.authVerifySource();
            case SCOPE_CHECKOUT_INITIATION_ACCOUNT -> properties.checkoutInitiationAccount();
            case SCOPE_CHECKOUT_INITIATION_SOURCE -> properties.checkoutInitiationSource();
            case SCOPE_SEARCH_SOURCE -> properties.searchSource();
            default -> null;
        };
        if (rule == null) {
            // Fails open, loudly: a typed scope constant is a bug worth seeing in logs, not an
            // outage for callers of a scope this class does not yet recognise.
            log.warn("No rate limit rule configured for unrecognised scope \"{}\"; allowing", scope);
            return new Rule(Integer.MAX_VALUE, Duration.ofMinutes(1));
        }
        return rule;
    }

    private static Instant flooredTo(Instant instant, Duration window) {
        long windowSeconds = Math.max(1, window.toSeconds());
        long flooredEpochSecond = (instant.getEpochSecond() / windowSeconds) * windowSeconds;
        return Instant.ofEpochSecond(flooredEpochSecond);
    }
}
