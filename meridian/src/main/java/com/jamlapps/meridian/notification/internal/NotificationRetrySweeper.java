package com.jamlapps.meridian.notification.internal;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BR-NTF-05: retries a FAILED message without anything driving it from outside — structured
 * exactly like {@code tax.internal.TaxRetrySweeper}, calling through the injected, proxied {@link
 * NotificationService} rather than looping from inside that class.
 */
@Component
class NotificationRetrySweeper {

    private static final Logger log = LoggerFactory.getLogger(NotificationRetrySweeper.class);

    private final NotificationService service;

    NotificationRetrySweeper(NotificationService service) {
        this.service = service;
    }

    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        List<UUID> messageIds = service.retryableMessageIds();
        for (UUID messageId : messageIds) {
            service.transmit(messageId);
        }
        if (!messageIds.isEmpty()) {
            log.info("Notification retry sweep attempted {} message(s)", messageIds.size());
        }
    }
}
