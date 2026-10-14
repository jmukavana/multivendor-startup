package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.Delivery;
import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-FUL-01/02/04/06/07: one row per sub-order needing delivery — see {@code OrderReadinessListener} for how it's created. */
@Entity
@Table(name = "delivery")
class DeliveryEntity {

    @Id
    private UUID id;

    @Column(name = "sub_order_id", nullable = false)
    private UUID subOrderId;

    @Column(name = "parent_order_id", nullable = false)
    private UUID parentOrderId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "buyer_account_id", nullable = false)
    private UUID buyerAccountId;

    @Column(name = "carrier_id")
    private @Nullable UUID carrierId;

    @Column(name = "zone_id")
    private @Nullable UUID zoneId;

    @Enumerated(EnumType.STRING)
    @Column
    private @Nullable DeliveryMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    @Column(name = "tracking_reference")
    private @Nullable String trackingReference;

    @Column(name = "proof_of_delivery_reference")
    private @Nullable String proofOfDeliveryReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "booked_at")
    private @Nullable Instant bookedAt;

    @Column(name = "dispatched_at")
    private @Nullable Instant dispatchedAt;

    @Column(name = "delivered_at")
    private @Nullable Instant deliveredAt;

    protected DeliveryEntity() {
        // for JPA
    }

    DeliveryEntity(UUID id, UUID subOrderId, UUID parentOrderId, UUID vendorId, UUID buyerAccountId, Instant createdAt) {
        this.id = id;
        this.subOrderId = subOrderId;
        this.parentOrderId = parentOrderId;
        this.vendorId = vendorId;
        this.buyerAccountId = buyerAccountId;
        this.status = DeliveryStatus.PENDING;
        this.createdAt = createdAt;
    }

    /** BR-FUL-01/03. */
    void book(UUID carrierId, UUID zoneId, DeliveryMethod method, Instant now) {
        this.carrierId = carrierId;
        this.zoneId = zoneId;
        this.method = method;
        this.status = DeliveryStatus.BOOKED;
        this.bookedAt = now;
    }

    void fail() {
        this.status = DeliveryStatus.FAILED;
    }

    /** BR-FUL-04: dispatch, with a tracking reference. Also the recovery path from FAILED (AC-FUL-04's "manual dispatch option"). */
    void dispatch(String trackingReference, Instant now) {
        this.status = DeliveryStatus.DISPATCHED;
        this.trackingReference = trackingReference;
        this.dispatchedAt = now;
    }

    /** AC-FUL-02: for PICKUP_POINT, arrival at the agent point — status stays In Transit until collection is recorded separately. */
    void markInTransit() {
        this.status = DeliveryStatus.IN_TRANSIT;
    }

    /** BR-FUL-06/07. */
    void markDelivered(@Nullable String proofOfDeliveryReference, Instant now) {
        this.status = DeliveryStatus.DELIVERED;
        this.proofOfDeliveryReference = proofOfDeliveryReference;
        this.deliveredAt = now;
    }

    UUID id() {
        return id;
    }

    UUID subOrderId() {
        return subOrderId;
    }

    UUID parentOrderId() {
        return parentOrderId;
    }

    UUID vendorId() {
        return vendorId;
    }

    UUID buyerAccountId() {
        return buyerAccountId;
    }

    DeliveryStatus status() {
        return status;
    }

    @Nullable UUID carrierId() {
        return carrierId;
    }

    @Nullable String trackingReference() {
        return trackingReference;
    }

    Delivery toDelivery() {
        return new Delivery(
                id,
                subOrderId,
                parentOrderId,
                vendorId,
                status,
                carrierId,
                zoneId,
                method,
                trackingReference,
                proofOfDeliveryReference,
                createdAt,
                dispatchedAt,
                deliveredAt);
    }
}
