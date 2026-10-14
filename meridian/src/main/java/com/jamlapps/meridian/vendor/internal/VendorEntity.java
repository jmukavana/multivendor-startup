package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.Vendor;
import com.jamlapps.meridian.vendor.VendorStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "vendor")
class VendorEntity {

    @Id
    private UUID id;

    @Column(name = "owner_account_id", nullable = false)
    private UUID ownerAccountId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorStatus status;

    @Column(name = "suspended_at")
    private @Nullable Instant suspendedAt;

    @Column(name = "suspension_reason")
    private @Nullable String suspensionReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected VendorEntity() {
        // for JPA
    }

    /** Only ever called from application approval now — see {@code VendorApplicationService}. */
    VendorEntity(UUID id, UUID ownerAccountId, String businessName, Instant createdAt) {
        this.id = id;
        this.ownerAccountId = ownerAccountId;
        this.businessName = businessName;
        this.status = VendorStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    void suspend(String reason, Instant now) {
        this.status = VendorStatus.SUSPENDED;
        this.suspensionReason = reason;
        this.suspendedAt = now;
    }

    Vendor toVendor() {
        return new Vendor(id, ownerAccountId, businessName, status, createdAt);
    }

    UUID id() {
        return id;
    }

    UUID ownerAccountId() {
        return ownerAccountId;
    }

    VendorStatus status() {
        return status;
    }
}
