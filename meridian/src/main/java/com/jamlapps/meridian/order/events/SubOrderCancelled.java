package com.jamlapps.meridian.order.events;

import com.jamlapps.meridian.shared.Money;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * BR-ORD-05/13/14: a sub-order was cancelled before it was delivered — by the buyer (BR-ORD-13),
 * or by the system because the vendor let BR-ORD-05's acceptance window lapse without accepting
 * it. Three modules already depend on {@code order::events} and each reacts to a different
 * consequence, none of them needing a further dependency to do it: {@code inventory} releases or
 * restocks {@code lines} (RELEASE for a reservation still RESERVED, restock for one already
 * CONFIRMED — which applies is the reservation's own state, not carried here); {@code ledger}
 * reverses commission via {@code LedgerService.postRefund} when {@code paymentWasConfirmed};
 * {@code notification} tells the vendor, using {@code reason} to say which of the two this was.
 * {@code refundAmount}, {@code commissionRate}/{@code commissionConfigurationId} and {@code
 * holdbackRate}/{@code holdbackConfigurationId} are only meaningful when {@code
 * paymentWasConfirmed} is true — a sub-order still PENDING_PAYMENT was never charged and has no
 * ledger posting to reverse.
 *
 * @param refundAmount deliberately the sub-order's items subtotal, not {@code SubOrder.total()} —
 *     {@code ledger.internal.LedgerService.postRefund} computes commission directly off this
 *     figure, and delivery is never commission-bearing (it is a pass-through carrier charge, not
 *     vendor revenue); {@code tax.internal.CancellationCreditNoteListener} uses the same figure so
 *     a credit note stays consistent with what {@code InvoiceGenerationListener} originally
 *     invoiced, which is also items-only (BR-CHK-06/07's delivery cost is not yet reflected in
 *     REG-04's eTIMS invoice at all — a real, separate gap this event does not attempt to close).
 *     The money actually returned to the buyer is a different, larger figure — {@code
 *     OrderService.cancelInternal} refunds {@code SubOrder.total()} through {@code
 *     payment.RefundInitiation} directly, not through this event.
 * @param reason the same string {@code payment.RefundInitiation} was given, so the notification a
 *     vendor reads and the reason a refund was issued for never disagree
 */
public record SubOrderCancelled(
        UUID subOrderId,
        UUID parentOrderId,
        UUID vendorId,
        UUID buyerAccountId,
        boolean paymentWasConfirmed,
        Money refundAmount,
        BigDecimal commissionRate,
        UUID commissionConfigurationId,
        BigDecimal holdbackRate,
        UUID holdbackConfigurationId,
        List<CancelledLine> lines,
        String reason,
        Instant cancelledAt) {

    public SubOrderCancelled {
        lines = List.copyOf(lines);
    }

    public record CancelledLine(UUID productId, int quantity) {}
}
