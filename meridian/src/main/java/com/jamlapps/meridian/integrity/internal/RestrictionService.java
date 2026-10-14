package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.Restriction;
import com.jamlapps.meridian.integrity.RestrictionStatus;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-SEC-07/08, AC-SEC-02: the admin-facing half of restriction handling — {@link RateLimiterService} only ever creates one. */
@Service
class RestrictionService {

    private final RestrictionRepository restrictions;
    private final IntegrityEventRepository events;
    private final Clock clock;

    RestrictionService(RestrictionRepository restrictions, IntegrityEventRepository events, Clock clock) {
        this.restrictions = restrictions;
        this.events = events;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    List<Restriction> list(RestrictionStatus status) {
        return restrictions.findByStatusOrderByCreatedAtDesc(status).stream().map(RestrictionEntity::toRestriction).toList();
    }

    /** BR-SEC-07: full reversal only, by an admin, logged as a manual override (BR-SEC-08). */
    @Transactional
    Restriction lift(UUID restrictionId, UUID liftedBy, String reason) {
        RestrictionEntity restriction =
                restrictions.findById(restrictionId).orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "No such restriction."));

        if (restriction.status() != RestrictionStatus.ACTIVE) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This restriction is not active.");
        }

        Instant now = clock.instant();
        restriction.lift(liftedBy, reason, now);
        restrictions.save(restriction);

        events.save(new IntegrityEventEntity(
                UUID.randomUUID(),
                "RESTRICTION_LIFTED",
                restriction.scope(),
                restriction.key(),
                reason,
                liftedBy,
                now));

        return restriction.toRestriction();
    }
}
