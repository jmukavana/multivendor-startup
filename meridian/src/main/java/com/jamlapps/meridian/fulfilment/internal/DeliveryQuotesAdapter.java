package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryQuote;
import com.jamlapps.meridian.fulfilment.DeliveryQuotes;
import com.jamlapps.meridian.fulfilment.DeliveryZone;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class DeliveryQuotesAdapter implements DeliveryQuotes {

    private final DeliveryZoneRepository zones;
    private final CarrierRepository carriers;
    private final DeliveryRateCardRepository rateCards;

    DeliveryQuotesAdapter(DeliveryZoneRepository zones, CarrierRepository carriers, DeliveryRateCardRepository rateCards) {
        this.zones = zones;
        this.carriers = carriers;
        this.rateCards = rateCards;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryZone> activeZones() {
        return zones.findByActiveTrue().stream().map(DeliveryZoneEntity::toDeliveryZone).toList();
    }

    /**
     * No single query does this — rate cards are versioned per (carrier, zone, method), so "the
     * cheapest current one" means resolving {@code findCurrent} once per active carrier and
     * comparing what comes back, the same per-carrier resolution {@code DeliveryService
     * .currentRate} already does for one carrier at a time. Fine at this scale: a handful of
     * active carriers, not hundreds.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryQuote> cheapestQuote(UUID zoneId, DeliveryMethod method) {
        return carriers.findByActiveTrue().stream()
                .flatMap(carrier -> rateCards.findCurrent(carrier.id(), zoneId, method).stream())
                .map(DeliveryRateCardEntity::toDeliveryRateCard)
                .min(Comparator.comparingLong(rateCard -> rateCard.price().minorUnits()))
                .map(rateCard -> new DeliveryQuote(rateCard.carrierId(), rateCard.id(), rateCard.price()));
    }
}
