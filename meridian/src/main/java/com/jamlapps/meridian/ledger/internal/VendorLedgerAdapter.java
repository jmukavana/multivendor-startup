package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.ledger.VendorLedger;
import com.jamlapps.meridian.ledger.VendorLedgerBalance;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class VendorLedgerAdapter implements VendorLedger {

    private final LedgerService ledger;

    VendorLedgerAdapter(LedgerService ledger) {
        this.ledger = ledger;
    }

    @Override
    public VendorLedgerBalance balanceFor(UUID vendorId, Currency currency) {
        return new VendorLedgerBalance(
                vendorId,
                ledger.balance(vendorId, LedgerAccount.VENDOR_PAYABLE, currency),
                ledger.balance(vendorId, LedgerAccount.HOLDBACK_RESERVE, currency));
    }

    @Override
    public List<UUID> vendorsWithPayableBalance(Currency currency) {
        return ledger.vendorsWithPayableBalance(currency);
    }
}
