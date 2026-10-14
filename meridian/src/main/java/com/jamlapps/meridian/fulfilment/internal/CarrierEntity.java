package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.Carrier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * BR-FUL-01/03. Seeded by V19's migration only — no write path yet. BR-FUL-03's actual
 * requirement ("multiple carriers... allowing carriers to be added without changes to order
 * processing") is a structural claim about this module's design, not a promise of an admin CRUD
 * screen; AC-FUL-03 is satisfied by a second row existing, however it got there. Unlike {@link
 * DeliveryZoneEntity}, which BR-FUL-08 does explicitly ask admins to be able to configure.
 */
@Entity
@Table(name = "carrier")
class CarrierEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CarrierEntity() {
        // for JPA
    }

    UUID id() {
        return id;
    }

    Carrier toCarrier() {
        return new Carrier(id, name, active);
    }
}
