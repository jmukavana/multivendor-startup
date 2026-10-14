package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.VendorApplication;
import com.jamlapps.meridian.vendor.VendorApplicationStatus;
import com.jamlapps.meridian.vendor.events.VendorApplicationApproved;
import com.jamlapps.meridian.vendor.events.VendorApplicationStatusChanged;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.AuditLog;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-VND-01 to 07: submission through approval or rejection.
 *
 * <p>UC-01's Draft state is not reachable from these endpoints — see
 * {@code VendorApplicationEntity}'s constructor comment for why an application is created already
 * Submitted.
 */
@Service
class VendorApplicationService {

    private static final List<VendorApplicationStatus> LIVE_STATUSES = List.of(
            VendorApplicationStatus.SUBMITTED,
            VendorApplicationStatus.UNDER_REVIEW,
            VendorApplicationStatus.INFORMATION_REQUESTED,
            VendorApplicationStatus.APPROVED);

    private static final List<VendorApplicationStatus> QUEUE_STATUSES = List.of(
            VendorApplicationStatus.SUBMITTED,
            VendorApplicationStatus.UNDER_REVIEW,
            VendorApplicationStatus.INFORMATION_REQUESTED);

    private final VendorApplicationRepository applications;
    private final VendorRepository vendors;
    private final ApplicationEventPublisher events;
    private final AuditLog auditLog;
    private final Clock clock;

    VendorApplicationService(
            VendorApplicationRepository applications,
            VendorRepository vendors,
            ApplicationEventPublisher events,
            AuditLog auditLog,
            Clock clock) {
        this.applications = applications;
        this.vendors = vendors;
        this.events = events;
        this.auditLog = auditLog;
        this.clock = clock;
    }

    /** BR-VND-01/02/04, AC-VND-05. */
    @Transactional
    VendorApplication submit(
            UUID accountId,
            String businessName,
            @Nullable String registrationNumber,
            String kraPin,
            String physicalAddress,
            String county,
            String contactName,
            String contactPhone,
            @Nullable String contactEmail) {

        if (applications.existsByKraPinAndStatusIn(kraPin, LIVE_STATUSES)) {
            throw new ApiException(
                    ErrorCode.KRA_PIN_TAKEN,
                    "This KRA PIN is already registered to a vendor or a pending application.");
        }

        Instant now = clock.instant();

        try {
            VendorApplicationEntity entity = applications.saveAndFlush(new VendorApplicationEntity(
                    UUID.randomUUID(),
                    accountId,
                    businessName,
                    registrationNumber,
                    kraPin,
                    physicalAddress,
                    county,
                    contactName,
                    contactPhone,
                    contactEmail,
                    now));

            publishStatusChange(entity, null, now);
            return toResponse(entity);
        } catch (DataIntegrityViolationException collision) {
            // The existsBy check above and the unique index in V6 are a check-then-act; a
            // concurrent submission of the same PIN between the two is rare but not impossible,
            // and the database is the actual authority here.
            throw new ApiException(
                    ErrorCode.KRA_PIN_TAKEN,
                    "This KRA PIN is already registered to a vendor or a pending application.");
        }
    }

    @Transactional(readOnly = true)
    VendorApplication get(UUID applicationId, UUID callerAccountId) {
        // Same reasoning as OrderService.get: confirming the id exists but belongs to someone else
        // is itself a disclosure. Enforced in the WHERE clause via findByIdAndAccountId, not by
        // fetching the row and checking it here — see that method's javadoc.
        VendorApplicationEntity entity = applications
                .findByIdAndAccountId(applicationId, callerAccountId)
                .orElseThrow(() -> ApiException.notFound("Vendor application", applicationId));

        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    List<VendorApplication> listMine(UUID accountId) {
        return applications.findByAccountIdOrderByCreatedAtDesc(accountId).stream()
                .map(this::toResponse)
                .toList();
    }

    /** BR-ADM-01. */
    @Transactional(readOnly = true)
    Page<VendorApplication> queue(Pageable pageable) {
        return applications.findByStatusInOrderBySubmittedAtAsc(QUEUE_STATUSES, pageable).map(this::toResponse);
    }

    @Transactional
    VendorApplication moveToUnderReview(UUID applicationId, Instant now) {
        VendorApplicationEntity entity = requireQueued(applicationId);
        entity.moveToUnderReview(now);
        publishStatusChange(entity, null, now);
        return toResponse(entity);
    }

    @Transactional
    VendorApplication requestInformation(UUID applicationId, String note, Instant now) {
        VendorApplicationEntity entity = requireQueued(applicationId);
        entity.requestInformation(note, now);
        publishStatusChange(entity, note, now);
        return toResponse(entity);
    }

    /** BR-VND-04, AC-VND-01: creates the operational vendor and notifies within the SLA. */
    @Transactional
    VendorApplication approve(UUID applicationId, UUID reviewerAccountId, Instant now) {
        VendorApplicationEntity entity = requireQueued(applicationId);

        VendorEntity vendor =
                vendors.save(new VendorEntity(UUID.randomUUID(), entity.accountId(), entity.businessName(), now));
        entity.approve(reviewerAccountId, vendor.id(), now);

        events.publishEvent(
                new VendorApplicationApproved(entity.id(), entity.accountId(), vendor.id(), entity.businessName(), now));
        publishStatusChange(entity, null, now);

        // BR-ADM-07: REQUIRES_NEW inside AuditLogService — survives regardless of what runs after this.
        auditLog.record(reviewerAccountId, "VENDOR_APPLICATION_APPROVED", "VendorApplication", applicationId.toString(), entity.businessName());

        return toResponse(entity);
    }

    /** BR-VND-06, AC-VND-06: a reason is required, enforced here rather than only by bean validation. */
    @Transactional
    VendorApplication reject(UUID applicationId, UUID reviewerAccountId, String reason, Instant now) {
        if (reason.isBlank()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A rejection reason is required.");
        }

        VendorApplicationEntity entity = requireQueued(applicationId);
        entity.reject(reviewerAccountId, reason, now);
        publishStatusChange(entity, reason, now);

        auditLog.record(reviewerAccountId, "VENDOR_APPLICATION_REJECTED", "VendorApplication", applicationId.toString(), reason);

        return toResponse(entity);
    }

    private VendorApplicationEntity requireQueued(UUID applicationId) {
        VendorApplicationEntity entity = applications
                .findById(applicationId)
                .orElseThrow(() -> ApiException.notFound("Vendor application", applicationId));

        if (entity.status() == VendorApplicationStatus.APPROVED || entity.status() == VendorApplicationStatus.REJECTED) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "Application " + applicationId + " has already been decided.");
        }

        return entity;
    }

    private void publishStatusChange(VendorApplicationEntity entity, @Nullable String reason, Instant now) {
        events.publishEvent(new VendorApplicationStatusChanged(
                entity.id(), entity.accountId(), entity.status(), reason, now));
    }

    private VendorApplication toResponse(VendorApplicationEntity entity) {
        var vendorStatus = entity.vendorId() == null
                ? null
                : vendors.findById(entity.vendorId()).map(VendorEntity::status).orElse(null);
        return entity.toVendorApplication(vendorStatus);
    }
}
