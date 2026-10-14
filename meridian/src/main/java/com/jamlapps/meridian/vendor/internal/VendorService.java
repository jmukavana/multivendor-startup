package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.Vendor;
import com.jamlapps.meridian.vendor.events.VendorSuspended;
import com.jamlapps.meridian.web.ApiException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-VND-10: suspension. Vendor creation moved to {@link VendorApplicationService} in Phase 2 — see
 * {@code Vendor}'s javadoc; there is no self-service path any more.
 */
@Service
class VendorService {

    private final VendorRepository repository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    VendorService(VendorRepository repository, ApplicationEventPublisher events, Clock clock) {
        this.repository = repository;
        this.events = events;
        this.clock = clock;
    }

    /**
     * Publishes {@link VendorSuspended} so {@code catalog} can delist without this module needing
     * to know anything about products — see that event's javadoc and ADR-004.
     *
     * <p>No reactivation path exists: BR-VND does not describe one, and auto-restoring a suspended
     * vendor's listings without review is not a default this platform should guess at.
     */
    @Transactional
    Vendor suspend(UUID vendorId, String reason) {
        VendorEntity vendor =
                repository.findById(vendorId).orElseThrow(() -> ApiException.notFound("Vendor", vendorId));

        Instant now = clock.instant();
        vendor.suspend(reason, now);
        repository.save(vendor);

        events.publishEvent(new VendorSuspended(vendorId, reason, now));

        return vendor.toVendor();
    }
}
