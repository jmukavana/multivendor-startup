package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliverySelections;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class DeliverySelectionsAdapter implements DeliverySelections {

    private final DeliverySelectionRepository selections;
    private final Clock clock;

    DeliverySelectionsAdapter(DeliverySelectionRepository selections, Clock clock) {
        this.selections = selections;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void select(UUID subOrderId, UUID carrierId, UUID zoneId, DeliveryMethod method, UUID rateCardId) {
        selections.save(new DeliverySelectionEntity(subOrderId, carrierId, zoneId, method, rateCardId, clock.instant()));
    }
}
