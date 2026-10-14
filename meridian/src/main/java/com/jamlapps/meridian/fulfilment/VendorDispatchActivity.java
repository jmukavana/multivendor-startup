package com.jamlapps.meridian.fulfilment;

import java.util.List;
import java.util.UUID;

/** BR-SEC-05: the "dispatch failure rate" half of the vendor-fraud signal. */
public interface VendorDispatchActivity {

    List<VendorDispatchStat> dispatchStats();

    record VendorDispatchStat(UUID vendorId, long totalDeliveries, long failedDeliveries) {}
}
