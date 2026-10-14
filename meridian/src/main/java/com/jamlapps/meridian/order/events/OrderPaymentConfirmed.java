package com.jamlapps.meridian.order.events;

import com.jamlapps.meridian.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A parent order's payment has been marked confirmed.
 *
 * <p>{@code subOrders} carries a resolved commission rate and holdback rate per vendor group, not
 * just the configuration version ids — {@code ledger} depends only on this module's events (see
 * {@code ledger}'s {@code package-info.java}), not on {@code pricing}, so this event has to be
 * self-contained for {@code ledger} to post commission, holdback and vendor-payable entries
 * without a further module dependency. {@code order} already resolved both rates at order
 * creation (COM-05, BR-ADM-06); these are those same rates, carried along rather than re-resolved.
 */
public record OrderPaymentConfirmed(
        UUID parentOrderId,
        String reference,
        UUID buyerAccountId,
        List<SubOrderSettlementDetail> subOrders,
        Instant confirmedAt) {

    public OrderPaymentConfirmed {
        subOrders = List.copyOf(subOrders);
    }

    public record SubOrderSettlementDetail(
            UUID subOrderId,
            UUID vendorId,
            Money subtotal,
            BigDecimal commissionRate,
            UUID commissionConfigurationId,
            BigDecimal holdbackRate,
            UUID holdbackConfigurationId) {}
}
