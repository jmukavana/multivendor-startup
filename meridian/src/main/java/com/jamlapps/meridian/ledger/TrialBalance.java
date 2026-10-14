package com.jamlapps.meridian.ledger;

/**
 * BR-LDG-05: whether the whole ledger currently sums to zero.
 *
 * <p>{@link com.jamlapps.meridian.ledger.internal.LedgerService#post} already refuses to write any
 * posting that does not balance in isolation, which makes this true by construction as long as
 * every entry ever written passed through that method. It can never be false through ordinary
 * application behaviour — the only way it goes false is a write that bypassed the application
 * entirely (a direct SQL statement against a running database), which is exactly the threat
 * BR-LDG-04's "no modification... through the application" implies exists and a DBA could still do.
 * BR-STL-10 checks this before any settlement run may proceed (AC-STL-02) precisely because that
 * threat, however unlikely, is the one case settlement must never disburse against.
 */
public interface TrialBalance {

    /** True when every debit across the whole ledger equals every credit. */
    boolean isReconciled();
}
