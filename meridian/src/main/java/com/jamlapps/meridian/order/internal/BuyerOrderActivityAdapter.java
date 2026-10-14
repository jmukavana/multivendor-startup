package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.BuyerOrderActivity;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class BuyerOrderActivityAdapter implements BuyerOrderActivity {

    private final SubOrderRepository subOrders;

    BuyerOrderActivityAdapter(SubOrderRepository subOrders) {
        this.subOrders = subOrders;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BuyerActivityStat> cancellationStats() {
        return subOrders.cancellationStatsByBuyer().stream()
                .map(row -> new BuyerActivityStat(row.buyerAccountId(), row.total(), row.cancelled()))
                .toList();
    }
}
