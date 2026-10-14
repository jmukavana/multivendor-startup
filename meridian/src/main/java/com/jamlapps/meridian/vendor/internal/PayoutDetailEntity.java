package com.jamlapps.meridian.vendor.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-VND-08/09. One live record per vendor — see V6's comment for why no history is kept yet. */
@Entity
@Table(name = "payout_detail")
class PayoutDetailEntity {

    /** BR-VND-09's indicative figure from BRD 5.9; not yet a configured value (OPEN-05). */
    static final Duration COOLING_OFF = Duration.ofDays(7);

    enum Method {
        MPESA,
        BANK
    }

    @Id
    private UUID id;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Method method;

    @Column(nullable = false)
    private String destination;

    @Column(name = "verified_at")
    private @Nullable Instant verifiedAt;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "cooling_off_until", nullable = false)
    private Instant coolingOffUntil;

    protected PayoutDetailEntity() {
        // for JPA
    }

    PayoutDetailEntity(UUID id, UUID vendorId, Method method, String destination, Instant now) {
        this.id = id;
        this.vendorId = vendorId;
        this.method = method;
        this.destination = destination;
        this.changedAt = now;
        this.coolingOffUntil = now.plus(COOLING_OFF);
    }

    /** BR-VND-09: any change resets the cooling-off window and clears prior verification. */
    void change(Method method, String destination, Instant now) {
        this.method = method;
        this.destination = destination;
        this.verifiedAt = null;
        this.changedAt = now;
        this.coolingOffUntil = now.plus(COOLING_OFF);
    }

    Instant coolingOffUntil() {
        return coolingOffUntil;
    }

    UUID vendorId() {
        return vendorId;
    }
}
