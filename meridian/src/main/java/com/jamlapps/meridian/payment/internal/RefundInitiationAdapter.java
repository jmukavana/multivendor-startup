package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.Refund;
import com.jamlapps.meridian.payment.RefundInitiation;
import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class RefundInitiationAdapter implements RefundInitiation {

    private final PaymentService service;

    RefundInitiationAdapter(PaymentService service) {
        this.service = service;
    }

    @Override
    public Refund initiate(UUID parentOrderId, Money amount, String reason) {
        return service.initiateRefund(parentOrderId, amount, reason);
    }
}
