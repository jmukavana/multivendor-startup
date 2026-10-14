package com.jamlapps.meridian.ledger;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

/**
 * BR-STL-06: settlement calls this directly (a synchronous cross-module call, like
 * {@code order.OrderCreation}) rather than an event, because settlement must know the ledger
 * posting succeeded before instructing disbursement to actually happen — the same "these two
 * things succeed or fail together" reasoning as checkout and order/inventory reservation.
 */
public interface LedgerPosting {

    /** DEBIT VENDOR_PAYABLE, CREDIT COLLECTIONS_CLEARING — cash actually leaves via the aggregator. */
    void postDisbursement(UUID vendorId, Money amount, UUID settlementRunId);

    /** DEBIT HOLDBACK_RESERVE, CREDIT VENDOR_PAYABLE — moves held funds back to disbursable. */
    void postHoldbackRelease(UUID vendorId, Money amount, UUID settlementRunId);
}
