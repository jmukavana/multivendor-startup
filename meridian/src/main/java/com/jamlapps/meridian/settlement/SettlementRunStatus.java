package com.jamlapps.meridian.settlement;

/** BR-STL-10/11: a run's lifecycle from reconciliation through disbursement. */
public enum SettlementRunStatus {

    /** BR-STL-10: reconciliation failed at initiation (AC-STL-02). No lines exist; nothing follows. */
    BLOCKED,

    /** Reconciled and its lines calculated; awaiting BR-STL-11's finance approval. */
    DRAFT,

    /** Approved (BR-STL-11) and disbursement underway or attempted for every line. */
    APPROVED,

    /** Every line has reached a final state: DISBURSED, EXCLUDED, or ESCALATED. */
    COMPLETED
}
