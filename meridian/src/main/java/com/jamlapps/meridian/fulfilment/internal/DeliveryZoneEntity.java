package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryZone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** BR-FUL-08. Admin-managed via {@code AdminFulfilmentController} — unlike {@code CarrierEntity}, which has no write path yet (see that entity's javadoc). */
@Entity
@Table(name = "delivery_zone")
class DeliveryZoneEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String county;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeliveryZoneEntity() {
        // for JPA
    }

    DeliveryZoneEntity(UUID id, String name, String county, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.county = county;
        this.active = true;
        this.createdAt = createdAt;
    }

    UUID id() {
        return id;
    }

    void deactivate() {
        this.active = false;
    }

    DeliveryZone toDeliveryZone() {
        return new DeliveryZone(id, name, county, active);
    }
}
