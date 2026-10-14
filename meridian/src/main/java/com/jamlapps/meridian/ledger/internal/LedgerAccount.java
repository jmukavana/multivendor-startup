package com.jamlapps.meridian.ledger.internal;

/**
 * BR-LDG-02's entry types, reduced to a chart of accounts. Debit-normal (COLLECTIONS_CLEARING) or
 * credit-normal (the rest) in the traditional sense, though nothing here enforces that beyond the
 * postings this module writes always balancing.
 *
 * <p>Two of BR-LDG-02's named entries are not modelled: delivery cost, because BR-CHK-06 (delivery
 * pricing) is deferred and nothing produces a real delivery charge to post; and refunds, because
 * BR-RTN (Phase 5) does not exist. Both are gaps to close when those modules land, not oversights
 * in this one.
 */
enum LedgerAccount {

    /** Asset: cash the aggregator holds on the platform's behalf, not yet disbursed. */
    COLLECTIONS_CLEARING,

    /** Revenue: the platform's own earned commission. */
    COMMISSION_REVENUE,

    /** Liability, per vendor: what the platform currently owes that vendor and can disburse. */
    VENDOR_PAYABLE,

    /** Liability, per vendor: the retained portion, owed but not yet disbursable. */
    HOLDBACK_RESERVE
}
