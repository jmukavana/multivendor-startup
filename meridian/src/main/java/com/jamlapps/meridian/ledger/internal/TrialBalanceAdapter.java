package com.jamlapps.meridian.ledger.internal;

import com.jamlapps.meridian.ledger.TrialBalance;
import org.springframework.stereotype.Component;

@Component
class TrialBalanceAdapter implements TrialBalance {

    private final LedgerService ledger;

    TrialBalanceAdapter(LedgerService ledger) {
        this.ledger = ledger;
    }

    @Override
    public boolean isReconciled() {
        return ledger.isReconciled();
    }
}
