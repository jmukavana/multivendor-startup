package com.jamlapps.meridian.tax;

/** REG-04 to 08: shared by {@link TaxInvoice} and {@link CreditNote} — the same transmission lifecycle applies to both. */
public enum TaxDocumentStatus {

    /** Generated, not yet transmitted to eTIMS. */
    PENDING,

    /** REG-07: retained with its acknowledgement reference. */
    TRANSMITTED,

    /** REG-05: an attempt failed; eligible for retry until MAX_ATTEMPTS is reached. */
    FAILED,

    /** REG-05/06: retry attempts exhausted — the dead-letter queue, visible in the admin exception queue. */
    DEAD_LETTERED
}
