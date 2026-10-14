package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.inventory.ReservationOutcome;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BR-INV-03, AC-INV-02: "when 15 minutes elapse without payment confirmation, the stock returns to
 * available and the reservation is closed" describes something that must happen without a buyer
 * or any other request driving it — a sweep, not a lazy check on read. Runs every 30 seconds,
 * which bounds how late an expiry can be caught relative to {@link InventoryService#RESERVATION_TTL}
 * without polling so tightly that idle periods cost anything.
 */
@Component
class ReservationExpirySweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpirySweeper.class);

    private final StockReservationRepository reservations;
    private final InventoryService inventory;
    private final Clock clock;

    ReservationExpirySweeper(StockReservationRepository reservations, InventoryService inventory, Clock clock) {
        this.reservations = reservations;
        this.inventory = inventory;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 30_000)
    void sweep() {
        var expired = reservations.findByStatusAndExpiresAtBefore(ReservationOutcome.RESERVED, clock.instant());

        // Each expiry runs in its own transaction (InventoryService.expire is @Transactional), so
        // one bad row cannot block the rest of the sweep, and a crash mid-sweep leaves already
        // processed reservations correctly resolved rather than rolling everything back.
        for (var reservation : expired) {
            inventory.expire(reservation.id());
        }

        if (!expired.isEmpty()) {
            log.info("Reservation expiry sweep released {} reservation(s)", expired.size());
        }
    }
}
