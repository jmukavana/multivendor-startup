package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.VendorApplication;
import com.jamlapps.meridian.vendor.VendorApplicationStatus;
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

/**
 * BR-VND-01 to 06: the application process.
 *
 * <p>{@code status} never stores {@code SUSPENDED} — the check constraint in V6 does not permit
 * it. A vendor's approval outlives an application row's own state, and its <em>current</em>
 * standing lives on the {@code vendor} row this application points to once approved. {@link
 * #toVendorApplication} computes the reported status from that vendor's status when one exists, so
 * the frontend still sees {@code SUSPENDED} in exactly the shape it expects
 * ({@code VendorApplicationStatus} in {@code types.ts}) without this entity needing to track two
 * sources of truth for the same fact.
 */
@Entity
@Table(name = "vendor_application")
class VendorApplicationEntity {

    @Id
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "business_name", nullable = false)
    private String businessName;

    @Column(name = "registration_number")
    private @Nullable String registrationNumber;

    @Column(name = "kra_pin", nullable = false)
    private String kraPin;

    @Column(name = "physical_address", nullable = false)
    private String physicalAddress;

    @Column(nullable = false)
    private String county;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "contact_email")
    private @Nullable String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorApplicationStatus status;

    @Column(name = "rejection_reason")
    private @Nullable String rejectionReason;

    @Column(name = "information_request")
    private @Nullable String informationRequest;

    @Column(name = "submitted_at")
    private @Nullable Instant submittedAt;

    @Column(name = "reviewed_at")
    private @Nullable Instant reviewedAt;

    @Column(name = "reviewer_account_id")
    private @Nullable UUID reviewerAccountId;

    @Column(name = "vendor_id")
    private @Nullable UUID vendorId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VendorApplicationEntity() {
        // for JPA
    }

    VendorApplicationEntity(
            UUID id,
            UUID accountId,
            String businessName,
            @Nullable String registrationNumber,
            String kraPin,
            String physicalAddress,
            String county,
            String contactName,
            String contactPhone,
            @Nullable String contactEmail,
            Instant now) {
        this.id = id;
        this.accountId = accountId;
        this.businessName = businessName;
        this.registrationNumber = registrationNumber;
        this.kraPin = kraPin;
        this.physicalAddress = physicalAddress;
        this.county = county;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        // BR-VND-04: routed to the admin queue immediately. UC-01's Draft state exists in the
        // requirement, but nothing in Phase 2 saves a draft before submission — the frontend
        // collects the whole form client-side and posts it complete (see actions.ts), so an
        // application is created already SUBMITTED rather than passing through Draft first.
        this.status = VendorApplicationStatus.SUBMITTED;
        this.submittedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }

    void moveToUnderReview(Instant now) {
        this.status = VendorApplicationStatus.UNDER_REVIEW;
        this.updatedAt = now;
    }

    void requestInformation(String note, Instant now) {
        this.status = VendorApplicationStatus.INFORMATION_REQUESTED;
        this.informationRequest = note;
        this.updatedAt = now;
    }

    void approve(UUID reviewerAccountId, UUID vendorId, Instant now) {
        this.status = VendorApplicationStatus.APPROVED;
        this.reviewerAccountId = reviewerAccountId;
        this.reviewedAt = now;
        this.vendorId = vendorId;
        this.updatedAt = now;
    }

    void reject(UUID reviewerAccountId, String reason, Instant now) {
        this.status = VendorApplicationStatus.REJECTED;
        this.reviewerAccountId = reviewerAccountId;
        this.rejectionReason = reason;
        this.reviewedAt = now;
        this.updatedAt = now;
    }

    VendorApplication toVendorApplication(@Nullable VendorStatus currentVendorStatus) {
        VendorApplicationStatus reportedStatus =
                currentVendorStatus == VendorStatus.SUSPENDED
                        ? VendorApplicationStatus.SUSPENDED
                        : status;

        return new VendorApplication(
                id,
                accountId,
                businessName,
                registrationNumber,
                kraPin,
                physicalAddress,
                county,
                contactName,
                contactPhone,
                contactEmail,
                reportedStatus,
                rejectionReason,
                informationRequest,
                submittedAt,
                vendorId);
    }

    UUID id() {
        return id;
    }

    UUID accountId() {
        return accountId;
    }

    String businessName() {
        return businessName;
    }

    String kraPin() {
        return kraPin;
    }

    VendorApplicationStatus status() {
        return status;
    }

    @Nullable UUID vendorId() {
        return vendorId;
    }
}
