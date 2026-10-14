package com.jamlapps.meridian.ledger;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

/** BR-LDG-06: a vendor's balance, derived entirely from ledger entries. */
public record VendorLedgerBalance(UUID vendorId, Money payable, Money holdback) {

    public Money totalOwed() {
        return payable.plus(holdback);
    }
}
