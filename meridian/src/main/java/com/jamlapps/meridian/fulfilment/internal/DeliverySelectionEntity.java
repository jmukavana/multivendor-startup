package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-CHK-05/06: the buyer's delivery choice, recorded before a {@link DeliveryEntity} row exists for the sub-order. */
@Entity
@Table(name = "delivery_selection")
class DeliverySelectionEntity {

    @Id
    @Column(name = "sub_order_id")
    private UUID subOrderId;

    @Column(name = "carrier_id", nullable = false)
    private UUID carrierId;

    @Column(name = "zone_id", nullable = false)
    private UUID zoneId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryMethod method;

    @Column(name = "rate_card_id", nullable = false)
    private UUID rateCardId;

    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;

    protected DeliverySelectionEntity() {
        // for JPA
    }

    DeliverySelectionEntity(UUID subOrderId, UUID carrierId, UUID zoneId, DeliveryMethod method, UUID rateCardId, Instant selectedAt) {
        this.subOrderId = subOrderId;
        this.carrierId = carrierId;
        this.zoneId = zoneId;
        this.method = method;
        this.rateCardId = rateCardId;
        this.selectedAt = selectedAt;
    }

    UUID carrierId() {
        return carrierId;
    }

    UUID zoneId() {
        return zoneId;
    }

    DeliveryMethod method() {
        return method;
    }
}
