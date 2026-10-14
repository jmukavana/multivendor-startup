package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.RestrictionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-SEC-07/08: a separate bean, not a method on {@link RateLimiterService}, for exactly the
 * reason {@code inventory.internal.OversellAttemptRecorder} is separate from {@code
 * InventoryService} — {@code tryConsume} runs inside its caller's transaction (Spring's default
 * propagation), and every caller that reaches this class does so because it is about to throw
 * {@code ApiException(RATE_LIMITED)}, which rolls that whole transaction back. Recording the
 * breach and any resulting restriction in the same transaction they are reporting a refusal
 * inside of would erase both the instant that transaction rolls back — an audit trail that never
 * survives the event it is auditing is not an audit trail. This bean's {@code REQUIRES_NEW} only
 * applies through the Spring proxy, so it must be called through the injected instance, never as
 * a self-invocation from {@link RateLimiterService}.
 */
@Component
class RateLimitBreachRecorder {

    private static final Logger log = LoggerFactory.getLogger(RateLimitBreachRecorder.class);

    static final int ESCALATION_THRESHOLD = 3;
    static final Duration ESCALATION_WINDOW = Duration.ofHours(1);

    private final IntegrityEventRepository events;
    private final RestrictionRepository restrictions;

    RateLimitBreachRecorder(IntegrityEventRepository events, RestrictionRepository restrictions) {
        this.events = events;
        this.restrictions = restrictions;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(String scope, String key, Instant now) {
        events.save(new IntegrityEventEntity(
                UUID.randomUUID(), "RATE_LIMIT_EXCEEDED", scope, key, "Rate limit exceeded for scope " + scope, null, now));

        boolean alreadyRestricted =
                restrictions.findByScopeAndKeyAndStatus(scope, key, RestrictionStatus.ACTIVE).isPresent();
        long recentBreaches = events.countByScopeAndKeyAndTypeAndOccurredAtAfter(scope, key, "RATE_LIMIT_EXCEEDED", now.minus(ESCALATION_WINDOW));

        if (!alreadyRestricted && recentBreaches >= ESCALATION_THRESHOLD) {
            restrictions.save(new RestrictionEntity(
                    UUID.randomUUID(),
                    scope,
                    key,
                    "Rate limit exceeded " + ESCALATION_THRESHOLD + "+ times within " + ESCALATION_WINDOW,
                    now));
            log.warn("Restriction created for scope {} key {} after repeated rate limit breaches", scope, key);
        }
    }
}
