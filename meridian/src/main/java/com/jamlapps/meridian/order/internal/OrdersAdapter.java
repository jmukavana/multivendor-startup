package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.Orders;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.SubOrderDetail;
import com.jamlapps.meridian.pricing.PricingConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class OrdersAdapter implements Orders {

    private final ParentOrderRepository parentOrders;
    private final SubOrderRepository subOrders;
    private final PricingConfiguration pricing;

    OrdersAdapter(ParentOrderRepository parentOrders, SubOrderRepository subOrders, PricingConfiguration pricing) {
        this.parentOrders = parentOrders;
        this.subOrders = subOrders;
        this.pricing = pricing;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParentOrder> findById(UUID parentOrderId) {
        return parentOrders.findById(parentOrderId).map(ParentOrderEntity::toParentOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentOrder> findByBuyerAccountId(UUID buyerAccountId) {
        return parentOrders
                .findByBuyerAccountIdOrderByCreatedAtDesc(buyerAccountId, Pageable.unpaged())
                .map(ParentOrderEntity::toParentOrder)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ParentOrder> findByReference(String reference) {
        return parentOrders.findByReference(reference).map(ParentOrderEntity::toParentOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParentOrder> findBySourceAddress(String sourceAddress) {
        return parentOrders.findBySourceAddressOrderByCreatedAtDesc(sourceAddress).stream()
                .map(ParentOrderEntity::toParentOrder)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<SubOrderDetail> findSubOrder(UUID subOrderId) {
        return subOrders.findById(subOrderId).map(subOrder -> new SubOrderDetail(
                subOrder.id(),
                subOrder.parentOrderId(),
                subOrder.vendorId(),
                subOrder.buyerAccountId(),
                subOrder.status(),
                subOrder.subtotal(),
                subOrder.lines().stream().map(OrderLineEntity::toOrderLine).toList(),
                pricing.rateForVersion(subOrder.commissionConfigurationId()).rate(),
                subOrder.commissionConfigurationId(),
                pricing.holdbackRateForVersion(subOrder.holdbackConfigurationId()).rate(),
                subOrder.holdbackConfigurationId(),
                subOrder.deliveredAt()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasDeliveredProduct(UUID buyerAccountId, UUID productId) {
        return subOrders.existsDeliveredLineForBuyerAndProduct(buyerAccountId, productId);
    }
}
