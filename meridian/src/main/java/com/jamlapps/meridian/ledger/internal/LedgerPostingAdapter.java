package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.ledger.LedgerPosting;
import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class LedgerPostingAdapter implements LedgerPosting {

    private final LedgerService ledger;

    LedgerPostingAdapter(LedgerService ledger) {
        this.ledger = ledger;
    }

    @Override
    public void postDisbursement(UUID vendorId, Money amount, UUID settlementRunId) {
        ledger.postDisbursement(vendorId, amount, settlementRunId);
    }

    @Override
    public void postHoldbackRelease(UUID vendorId, Money amount, UUID settlementRunId) {
        ledger.postHoldbackRelease(vendorId, amount, settlementRunId);
    }
}
