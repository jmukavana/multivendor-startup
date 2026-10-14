package com.jamlapps.meridian.ledger;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

/** BR-LDG-06. Read-only, matching every other cross-module lookup in this codebase. */
public interface VendorLedger {

    VendorLedgerBalance balanceFor(UUID vendorId, Currency currency);

    /**
     * BR-STL-01: the vendor ids a settlement run needs to consider — every vendor currently owed a
     * positive VENDOR_PAYABLE balance in {@code currency}. Settlement resolves each one's actual
     * amount via {@link #balanceFor}; this only answers "who", not "how much", because the two
     * calls happen at slightly different points in {@code SettlementService.initiate} and a single
     * combined call would force settlement to buffer amounts it might immediately discard for a
     * vendor later found to be excluded (BR-STL-08/09).
     */
    List<UUID> vendorsWithPayableBalance(Currency currency);
}
