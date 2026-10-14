package com.jamlapps.meridian.settlement;

/** A settlement line's outcome for one vendor within one run. */
public enum SettlementLineStatus {

    /** Calculated, not yet disbursed. */
    PENDING,

    /** BR-STL-08/09: excluded from this run; see the line's exclusionReason for why. */
    EXCLUDED,

    /** BR-STL-12: claimed for disbursement — set atomically so at most one attempt is ever in flight. */
    DISBURSING,

    /** BR-STL-06: the provider accepted the instruction and the ledger posting followed it. */
    DISBURSED,

    /** BR-STL-07: an attempt failed; eligible for retry until MAX_ATTEMPTS is reached. */
    FAILED,

    /** BR-STL-07: retry attempts exhausted; escalated to finance for manual resolution. */
    ESCALATED
}
