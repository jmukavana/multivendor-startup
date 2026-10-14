package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-VND-08/09: capture, verification-in-waiting, and cooling-off. BR-IAM-04, AC-VND-03: a vendor
 * staff caller must be refused, and the attempt logged whether it succeeds or not — the check and
 * the log are the same operation here rather than a separate audit step that could be skipped.
 */
@Service
class PayoutDetailService {

    private final PayoutDetailRepository payoutDetails;
    private final PayoutDetailAccessEventRepository accessEvents;
    private final VendorRepository vendors;
    private final Clock clock;

    PayoutDetailService(
            PayoutDetailRepository payoutDetails,
            PayoutDetailAccessEventRepository accessEvents,
            VendorRepository vendors,
            Clock clock) {
        this.payoutDetails = payoutDetails;
        this.accessEvents = accessEvents;
        this.vendors = vendors;
        this.clock = clock;
    }

    /** @param callerIsStaff true when the caller holds VENDOR_STAFF and not VENDOR_OWNER for this vendor */
    @Transactional
    PayoutDetailView view(UUID vendorId, UUID actorAccountId, boolean callerIsStaff) {
        requireVendor(vendorId);

        if (callerIsStaff) {
            log(vendorId, actorAccountId, PayoutDetailAccessEventEntity.Action.VIEW,
                    PayoutDetailAccessEventEntity.Outcome.REFUSED);
            throw new ApiException(ErrorCode.FORBIDDEN, "Vendor staff may not view payout details.");
        }

        log(vendorId, actorAccountId, PayoutDetailAccessEventEntity.Action.VIEW,
                PayoutDetailAccessEventEntity.Outcome.ALLOWED);

        return payoutDetails
                .findByVendorId(vendorId)
                .map(entity -> new PayoutDetailView(true, entity.coolingOffUntil()))
                .orElseGet(() -> new PayoutDetailView(false, null));
    }

    @Transactional
    void change(UUID vendorId, UUID actorAccountId, boolean callerIsStaff, PayoutDetailEntity.Method method, String destination) {
        requireVendor(vendorId);

        if (callerIsStaff) {
            log(vendorId, actorAccountId, PayoutDetailAccessEventEntity.Action.CHANGE,
                    PayoutDetailAccessEventEntity.Outcome.REFUSED);
            throw new ApiException(ErrorCode.FORBIDDEN, "Vendor staff may not modify payout details.");
        }

        Instant now = clock.instant();

        payoutDetails.findByVendorId(vendorId).ifPresentOrElse(
                existing -> {
                    existing.change(method, destination, now);
                    payoutDetails.save(existing);
                },
                () -> payoutDetails.save(new PayoutDetailEntity(UUID.randomUUID(), vendorId, method, destination, now)));

        log(vendorId, actorAccountId, PayoutDetailAccessEventEntity.Action.CHANGE,
                PayoutDetailAccessEventEntity.Outcome.ALLOWED);
    }

    private void requireVendor(UUID vendorId) {
        if (!vendors.existsById(vendorId)) {
            throw ApiException.notFound("Vendor", vendorId);
        }
    }

    private void log(
            UUID vendorId,
            UUID actorAccountId,
            PayoutDetailAccessEventEntity.Action action,
            PayoutDetailAccessEventEntity.Outcome outcome) {
        accessEvents.save(new PayoutDetailAccessEventEntity(
                UUID.randomUUID(), vendorId, actorAccountId, action, outcome, clock.instant()));
    }

    /** Whether details exist at all is itself sensitive-adjacent, so this never returns the raw destination. */
    record PayoutDetailView(boolean onFile, java.time.Instant coolingOffUntil) {}
}
