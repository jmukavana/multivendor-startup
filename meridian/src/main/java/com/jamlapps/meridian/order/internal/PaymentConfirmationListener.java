package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.payment.events.PaymentConfirmed;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-PAY-04/10: the only trigger for {@code OrderService.markPaymentConfirmed} — see its javadoc. */
@Component
class PaymentConfirmationListener {

    private final OrderService service;

    PaymentConfirmationListener(OrderService service) {
        this.service = service;
    }

    @ApplicationModuleListener
    void on(PaymentConfirmed event) {
        service.markPaymentConfirmed(event.parentOrderId(), null);
    }
}
