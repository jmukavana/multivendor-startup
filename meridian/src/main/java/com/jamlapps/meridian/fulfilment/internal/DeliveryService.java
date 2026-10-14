package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.Carrier;
import com.jamlapps.meridian.fulfilment.Delivery;
import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryRateCard;
import com.jamlapps.meridian.fulfilment.DeliveryStatus;
import com.jamlapps.meridian.fulfilment.DeliveryZone;
import com.jamlapps.meridian.fulfilment.events.DeliveryCompleted;
import com.jamlapps.meridian.fulfilment.events.DeliveryDispatched;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-FUL-01 to 08. */
@Service
class DeliveryService {

    private final DeliveryRepository deliveries;
    private final CarrierRepository carriers;
    private final DeliveryZoneRepository zones;
    private final DeliveryRateCardRepository rateCards;
    private final Vendors vendors;
    private final CarrierGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    DeliveryService(
            DeliveryRepository deliveries,
            CarrierRepository carriers,
            DeliveryZoneRepository zones,
            DeliveryRateCardRepository rateCards,
            Vendors vendors,
            CarrierGateway gateway,
            ApplicationEventPublisher events,
            Clock clock) {
        this.deliveries = deliveries;
        this.carriers = carriers;
        this.zones = zones;
        this.rateCards = rateCards;
        this.vendors = vendors;
        this.gateway = gateway;
        this.events = events;
        this.clock = clock;
    }

    /** BR-FUL-01, AC-FUL-04: a failed booking leaves the delivery FAILED, from which {@link #dispatchManually} is the recovery path. */
    @Transactional
    Delivery book(UUID deliveryId, UUID callerAccountId, UUID carrierId, UUID zoneId, DeliveryMethod method) {
        DeliveryEntity delivery = requireOwned(deliveryId, callerAccountId);

        if (delivery.status() != DeliveryStatus.PENDING && delivery.status() != DeliveryStatus.FAILED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Delivery " + deliveryId + " is not awaiting booking.");
        }
        if (!carriers.existsById(carrierId)) {
            throw ApiException.notFound("Carrier", carrierId);
        }
        if (!zones.existsById(zoneId)) {
            throw ApiException.notFound("DeliveryZone", zoneId);
        }

        var result = gateway.book(deliveryId, carrierId);
        Instant now = clock.instant();

        if (!result.accepted()) {
            delivery.fail();
            deliveries.save(delivery);
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Carrier booking failed for delivery " + deliveryId + "; use manual dispatch instead.");
        }

        delivery.book(carrierId, zoneId, method, now);
        deliveries.save(delivery);
        return delivery.toDelivery();
    }

    /**
     * BR-CHK-05/06: books a delivery with the buyer's own checkout-time selection — no vendor
     * action needed, unlike {@link #book}, which this deliberately does not reuse: that method's
     * {@code requireOwned} makes no sense for a system-driven call with no calling vendor. Silent,
     * not throwing, on an already-resolved delivery: {@code OrderReadinessListener} calls this
     * once, right after creating the delivery it applies to, so PENDING is the only status this is
     * ever meant to see — anything else would mean this ran twice, which recording nothing again is
     * the correct response to, not an error.
     */
    @Transactional
    Delivery autoBook(UUID deliveryId, UUID carrierId, UUID zoneId, DeliveryMethod method) {
        DeliveryEntity delivery = deliveries.findById(deliveryId).orElseThrow(() -> ApiException.notFound("Delivery", deliveryId));

        if (delivery.status() != DeliveryStatus.PENDING) {
            return delivery.toDelivery();
        }

        var result = gateway.book(deliveryId, carrierId);
        Instant now = clock.instant();

        if (!result.accepted()) {
            delivery.fail();
            deliveries.save(delivery);
            return delivery.toDelivery();
        }

        delivery.book(carrierId, zoneId, method, now);
        deliveries.save(delivery);
        return delivery.toDelivery();
    }

    /** BR-FUL-04, BR-NTF-01: publishes {@link DeliveryDispatched}. */
    @Transactional
    Delivery dispatch(UUID deliveryId, UUID callerAccountId) {
        DeliveryEntity delivery = requireOwned(deliveryId, callerAccountId);

        if (delivery.status() != DeliveryStatus.BOOKED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Delivery " + deliveryId + " is not booked.");
        }

        UUID carrierId = requireCarrierId(delivery);
        var result = gateway.dispatch(deliveryId, carrierId);
        Instant now = clock.instant();
        delivery.dispatch(result.trackingReference(), now);
        deliveries.save(delivery);

        events.publishEvent(new DeliveryDispatched(
                delivery.subOrderId(), delivery.parentOrderId(), delivery.id(), delivery.buyerAccountId(), result.trackingReference(), now));

        return delivery.toDelivery();
    }

    /**
     * BR-FUL-06: "manual status update by the vendor where carrier integration is unavailable" —
     * the actual requirement this satisfies, not a fallback bolted on afterward. Works from
     * PENDING, BOOKED or FAILED — a vendor may skip {@link #book} entirely and dispatch by hand.
     * BR-NTF-01: publishes {@link DeliveryDispatched} too — a manually-dispatched delivery is still
     * a real dispatch, and the buyer is told either way.
     */
    @Transactional
    Delivery dispatchManually(UUID deliveryId, UUID callerAccountId, String trackingReference) {
        DeliveryEntity delivery = requireOwned(deliveryId, callerAccountId);

        if (delivery.status() == DeliveryStatus.DISPATCHED
                || delivery.status() == DeliveryStatus.IN_TRANSIT
                || delivery.status() == DeliveryStatus.DELIVERED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Delivery " + deliveryId + " has already been dispatched.");
        }

        Instant now = clock.instant();
        delivery.dispatch(trackingReference, now);
        deliveries.save(delivery);

        events.publishEvent(new DeliveryDispatched(
                delivery.subOrderId(), delivery.parentOrderId(), delivery.id(), delivery.buyerAccountId(), trackingReference, now));

        return delivery.toDelivery();
    }

    /** AC-FUL-02: for PICKUP_POINT, arrival at the agent point — status stays In Transit until collection is recorded separately. */
    @Transactional
    Delivery markInTransit(UUID deliveryId, UUID callerAccountId) {
        DeliveryEntity delivery = requireOwned(deliveryId, callerAccountId);

        if (delivery.status() != DeliveryStatus.DISPATCHED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Delivery " + deliveryId + " is not dispatched.");
        }

        delivery.markInTransit();
        deliveries.save(delivery);
        return delivery.toDelivery();
    }

    /** BR-FUL-06/07, BR-NTF-01: publishes {@link DeliveryCompleted}, consumed by {@code notification} for the buyer's delivery notice. */
    @Transactional
    Delivery markDelivered(UUID deliveryId, UUID callerAccountId, @Nullable String proofOfDeliveryReference) {
        DeliveryEntity delivery = requireOwned(deliveryId, callerAccountId);

        if (delivery.status() != DeliveryStatus.DISPATCHED && delivery.status() != DeliveryStatus.IN_TRANSIT) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Delivery " + deliveryId + " is not out for delivery.");
        }

        Instant now = clock.instant();
        delivery.markDelivered(proofOfDeliveryReference, now);
        deliveries.save(delivery);

        events.publishEvent(
                new DeliveryCompleted(delivery.subOrderId(), delivery.parentOrderId(), delivery.id(), delivery.buyerAccountId(), now));

        return delivery.toDelivery();
    }

    /** AC-FUL-01: buyer or vendor, either side of the same delivery. */
    @Transactional(readOnly = true)
    Delivery get(UUID deliveryId, UUID callerAccountId) {
        DeliveryEntity delivery = deliveries.findById(deliveryId).orElseThrow(() -> ApiException.notFound("Delivery", deliveryId));

        boolean isVendorOwner = vendors.findByOwnerAccountId(callerAccountId).map(v -> v.id().equals(delivery.vendorId())).orElse(false);
        boolean isBuyer = delivery.buyerAccountId().equals(callerAccountId);

        if (!isVendorOwner && !isBuyer) {
            throw ApiException.notFound("Delivery", deliveryId);
        }

        return delivery.toDelivery();
    }

    @Transactional(readOnly = true)
    Delivery getBySubOrder(UUID subOrderId, UUID callerAccountId) {
        DeliveryEntity delivery =
                deliveries.findBySubOrderId(subOrderId).orElseThrow(() -> ApiException.notFound("Delivery", subOrderId));
        return get(delivery.id(), callerAccountId);
    }

    @Transactional(readOnly = true)
    List<Carrier> listCarriers() {
        return carriers.findByActiveTrue().stream().map(CarrierEntity::toCarrier).toList();
    }

    @Transactional(readOnly = true)
    List<DeliveryZone> listZones() {
        return zones.findByActiveTrue().stream().map(DeliveryZoneEntity::toDeliveryZone).toList();
    }

    /** BR-FUL-08. */
    @Transactional
    DeliveryZone createZone(String name, String county) {
        DeliveryZoneEntity zone = zones.save(new DeliveryZoneEntity(UUID.randomUUID(), name, county, clock.instant()));
        return zone.toDeliveryZone();
    }

    @Transactional
    void deactivateZone(UUID zoneId) {
        DeliveryZoneEntity zone = zones.findById(zoneId).orElseThrow(() -> ApiException.notFound("DeliveryZone", zoneId));
        zone.deactivate();
        zones.save(zone);
    }

    /** BR-FUL-08, DAT-07: a new versioned row, never an update to an existing one — matching {@code pricing}'s commission configuration exactly. */
    @Transactional
    DeliveryRateCard setRateCard(UUID carrierId, UUID zoneId, DeliveryMethod method, Money price) {
        if (!carriers.existsById(carrierId)) {
            throw ApiException.notFound("Carrier", carrierId);
        }
        if (!zones.existsById(zoneId)) {
            throw ApiException.notFound("DeliveryZone", zoneId);
        }

        Instant now = clock.instant();
        DeliveryRateCardEntity rateCard =
                rateCards.save(new DeliveryRateCardEntity(UUID.randomUUID(), carrierId, zoneId, method, price, now, now));
        return rateCard.toDeliveryRateCard();
    }

    @Transactional(readOnly = true)
    DeliveryRateCard currentRate(UUID carrierId, UUID zoneId, DeliveryMethod method) {
        return rateCards
                .findCurrent(carrierId, zoneId, method)
                .map(DeliveryRateCardEntity::toDeliveryRateCard)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR, "No rate card for carrier " + carrierId + ", zone " + zoneId + ", method " + method + "."));
    }

    private DeliveryEntity requireOwned(UUID deliveryId, UUID callerAccountId) {
        UUID vendorId = requireVendorId(callerAccountId);
        return deliveries.findByIdAndVendorId(deliveryId, vendorId).orElseThrow(() -> ApiException.notFound("Delivery", deliveryId));
    }

    private UUID requireVendorId(UUID callerAccountId) {
        return vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may manage a delivery."))
                .id();
    }

    private UUID requireCarrierId(DeliveryEntity delivery) {
        UUID carrierId = delivery.carrierId();
        if (carrierId == null) {
            throw new IllegalStateException("Delivery " + delivery.id() + " is BOOKED but has no carrierId.");
        }
        return carrierId;
    }
}
