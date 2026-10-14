package com.jamlapps.meridian.vendor.events;

import java.time.Instant;
import java.util.UUID;

/**
 * BR-VND-10: immediately delists the vendor's products. {@code catalog} listens and delists on
 * this event rather than {@code order} or {@code catalog} querying vendor status per read — see
 * ADR-004 and the listener's own javadoc for why.
 */
public record VendorSuspended(UUID vendorId, String reason, Instant occurredAt) {}
