package com.jamlapps.meridian.settlement.internal;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BR-STL-07: retries a FAILED line without anything driving it from outside. A separate component
 * from {@link SettlementService} for the same reason {@code inventory
 * .ReservationExpirySweeper} is separate from {@code InventoryService} — see {@code
 * SettlementService}'s own class doc on why the loop cannot live inside that class.
 */
@Component
class SettlementRetrySweeper {

    private static final Logger log = LoggerFactory.getLogger(SettlementRetrySweeper.class);

    private final SettlementService settlement;

    SettlementRetrySweeper(SettlementService settlement) {
        this.settlement = settlement;
    }

    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        List<UUID> retryable = settlement.retryableLineIds();

        for (UUID lineId : retryable) {
            settlement.disburseLine(lineId);
        }

        if (!retryable.isEmpty()) {
            log.info("Settlement retry sweep attempted {} line(s)", retryable.size());
        }
    }
}
