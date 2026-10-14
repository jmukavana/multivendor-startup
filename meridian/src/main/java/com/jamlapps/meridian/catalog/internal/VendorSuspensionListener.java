package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.vendor.events.VendorSuspended;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-VND-10: "immediately delisting their products". Listening for the event rather than checking
 * vendor status on every catalog read keeps the delisting query inside catalog's own table — see
 * the {@code vendor::events} note in this module's {@code package-info.java} and ADR-004.
 *
 * <p>There is no corresponding reactivation listener: BR-VND describes no path back from
 * suspension, so none is assumed here either — see {@code VendorService.suspend}'s javadoc.
 */
@Component
class VendorSuspensionListener {

    private static final Logger log = LoggerFactory.getLogger(VendorSuspensionListener.class);

    private final ProductRepository products;
    private final java.time.Clock clock;

    VendorSuspensionListener(ProductRepository products, java.time.Clock clock) {
        this.products = products;
        this.clock = clock;
    }

    @ApplicationModuleListener
    void on(VendorSuspended event) {
        int delisted = products.delistAllForVendor(event.vendorId(), clock.instant());
        log.info("Vendor {} suspended: delisted {} product(s)", event.vendorId(), delisted);
    }
}
