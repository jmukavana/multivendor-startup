package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.order.events.OrderPaymentConfirmed;
import com.jamlapps.meridian.shared.Money;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** REG-04: one invoice per parent order, summed across every sub-order the buyer's payment covered. */
@Component
class InvoiceGenerationListener {

    private final TaxService tax;

    InvoiceGenerationListener(TaxService tax) {
        this.tax = tax;
    }

    @ApplicationModuleListener
    void on(OrderPaymentConfirmed event) {
        Money total = event.subOrders().stream()
                .map(OrderPaymentConfirmed.SubOrderSettlementDetail::subtotal)
                .reduce(Money::plus)
                .orElseThrow(() -> new IllegalStateException("OrderPaymentConfirmed for " + event.parentOrderId() + " carried no sub-orders."));

        tax.generateInvoice(event.parentOrderId(), total);
    }
}
