package com.jamlapps.meridian.vendor.internal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.IdClass;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "vendor_staff")
@IdClass(VendorStaffEntity.Key.class)
class VendorStaffEntity {

    @Id
    @Column(name = "vendor_id")
    private UUID vendorId;

    @Id
    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected VendorStaffEntity() {
        // for JPA
    }

    VendorStaffEntity(UUID vendorId, UUID accountId, Instant addedAt) {
        this.vendorId = vendorId;
        this.accountId = accountId;
        this.addedAt = addedAt;
    }

    static class Key implements Serializable {
        UUID vendorId;
        UUID accountId;

        Key() {}

        Key(UUID vendorId, UUID accountId) {
            this.vendorId = vendorId;
            this.accountId = accountId;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Key other
                    && Objects.equals(vendorId, other.vendorId)
                    && Objects.equals(accountId, other.accountId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(vendorId, accountId);
        }
    }
}
