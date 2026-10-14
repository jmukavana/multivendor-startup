package com.jamlapps.meridian.fulfilment.internal;

import java.util.UUID;

/** BR-SEC-05: one vendor's aggregate delivery counts — {@code DeliveryRepository.dispatchStatsByVendor}'s JPQL constructor-expression target. */
record VendorDispatchRow(UUID vendorId, long total, long failed) {}
