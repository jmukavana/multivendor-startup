package com.jamlapps.meridian.notification;

/** BR-NTF-05: the same retry/dead-letter lifecycle {@code tax.TaxDocumentStatus} already established for this codebase. */
public enum NotificationStatus {

    /** Enqueued, not yet attempted — never actually observed at rest: the first attempt runs in the same transaction as enqueueing. */
    PENDING,

    /** Delivered to the gateway successfully. */
    SENT,

    /** An attempt failed; eligible for retry until {@code MAX_ATTEMPTS} is reached. */
    FAILED,

    /** BR-NTF-05's "log permanent failures": retry attempts exhausted, visible in the admin exception queue. */
    DEAD_LETTERED
}
