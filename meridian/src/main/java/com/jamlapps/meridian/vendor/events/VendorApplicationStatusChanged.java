package com.jamlapps.meridian.vendor.events;

import com.jamlapps.meridian.vendor.VendorApplicationStatus;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-VND-06: the applicant is notified of every state change, with a reason where rejected. */
public record VendorApplicationStatusChanged(
        UUID applicationId,
        UUID accountId,
        VendorApplicationStatus status,
        @Nullable String reason,
        Instant occurredAt) {}
