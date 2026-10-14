package com.jamlapps.meridian.vendor.events;

import java.time.Instant;
import java.util.UUID;

/** BR-VND-06, AC-VND-01: the applicant must be notified within 60 seconds of approval. */
public record VendorApplicationApproved(
        UUID applicationId, UUID accountId, UUID vendorId, String businessName, Instant approvedAt) {}
