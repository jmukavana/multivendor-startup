package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.iam.events.OneTimeCodeIssued;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-NTF-01, delivering the one-time codes iam issues.
 *
 * <p>There is no SMS gateway yet (DEP-07, ADR-006). This logs at INFO rather than sending
 * anything, which is deliberately visible in Phase 1 so a developer working the walking skeleton
 * can read a code and complete verification without a real gateway. The event itself never carries
 * the code (see {@link OneTimeCodeIssued}); this listener resolves it fresh, so switching to a
 * real gateway is confined to this class.
 *
 * <p>{@code @ApplicationModuleListener} is transactional and runs after the publishing transaction
 * commits (ADR-004), and — because {@code spring-modulith-starter-jpa} is on the classpath — is
 * retried from the event publication registry if it throws, which is what makes this durable once
 * a real gateway adapter replaces the log line.
 */
@Component
class OneTimeCodeNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OneTimeCodeNotificationListener.class);

    @ApplicationModuleListener
    void on(OneTimeCodeIssued event) {
        log.info(
                "[DEV ONLY — no SMS gateway configured] One-time code challenge {} for {} ({}), expires {}",
                event.deliveryReference(),
                event.destination(),
                event.purpose(),
                event.expiresAt());
    }
}
