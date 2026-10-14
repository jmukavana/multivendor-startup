package com.jamlapps.meridian.order.internal;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BR-ORD-05: auto-cancels a sub-order the vendor never accepted, without anything driving it from
 * outside — structured exactly like {@code tax.internal.TaxRetrySweeper}, calling through the
 * injected, proxied {@link OrderService} rather than looping from inside that class.
 */
@Component
class AcceptanceWindowSweeper {

    private static final Logger log = LoggerFactory.getLogger(AcceptanceWindowSweeper.class);

    private final OrderService orders;

    AcceptanceWindowSweeper(OrderService orders) {
        this.orders = orders;
    }

    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        List<UUID> candidates = orders.unacceptedPastDeadlineIds();
        for (UUID subOrderId : candidates) {
            orders.autoCancelUnaccepted(subOrderId);
        }
        if (!candidates.isEmpty()) {
            log.info("Acceptance window sweep auto-cancelled {} sub-order(s)", candidates.size());
        }
    }
}
