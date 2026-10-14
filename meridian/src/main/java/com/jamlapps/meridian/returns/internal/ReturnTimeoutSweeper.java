package com.jamlapps.meridian.returns.internal;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BR-RTN-03: "escalation to admin on rejection or timeout" — the timeout half, with nothing driving
 * it from outside. A separate component from {@link ReturnService}, matching {@code
 * inventory.ReservationExpirySweeper}'s and {@code settlement.SettlementRetrySweeper}'s reasoning
 * exactly: looping and calling {@code escalateIfTimedOut} from inside {@code ReturnService} itself
 * would bypass this bean's Spring proxy, and each escalation needs its own transaction so one bad
 * row cannot block the rest.
 */
@Component
class ReturnTimeoutSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReturnTimeoutSweeper.class);

    private final ReturnService returns;

    ReturnTimeoutSweeper(ReturnService returns) {
        this.returns = returns;
    }

    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        List<UUID> timedOut = returns.timedOutReturnIds();

        for (UUID returnId : timedOut) {
            returns.escalateIfTimedOut(returnId);
        }

        if (!timedOut.isEmpty()) {
            log.info("Return timeout sweep escalated {} return(s)", timedOut.size());
        }
    }
}
