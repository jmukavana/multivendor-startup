package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.OrderCreation;
import com.jamlapps.meridian.order.OrderLineRequest;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.RequestOrigin;
import com.jamlapps.meridian.shared.Money;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OrderCreationAdapter implements OrderCreation {

    private final OrderService service;

    OrderCreationAdapter(OrderService service) {
        this.service = service;
    }

    @Override
    public ParentOrder create(UUID buyerAccountId, List<OrderLineRequest> lines, Map<UUID, Money> deliveryCostByVendor, RequestOrigin origin) {
        return service.create(buyerAccountId, lines, deliveryCostByVendor, origin);
    }
}
