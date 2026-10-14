package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.BuyerReturnActivity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class BuyerReturnActivityAdapter implements BuyerReturnActivity {

    private final ReturnRepository returns;

    BuyerReturnActivityAdapter(ReturnRepository returns) {
        this.returns = returns;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuyerReturnStat> returnStats() {
        return returns.returnStatsByBuyer().stream()
                .map(row -> new BuyerReturnStat(row.buyerAccountId(), row.total(), row.disputed()))
                .toList();
    }
}
