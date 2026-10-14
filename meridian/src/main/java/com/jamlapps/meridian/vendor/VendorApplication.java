package com.jamlapps.meridian.vendor;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public record VendorApplication(
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
        VendorApplicationStatus status,
        @Nullable String rejectionReason,
        @Nullable String informationRequest,
        @Nullable Instant submittedAt,
        /** Set once approved — the operational vendor this application produced. */
        @Nullable UUID vendorId) {}
