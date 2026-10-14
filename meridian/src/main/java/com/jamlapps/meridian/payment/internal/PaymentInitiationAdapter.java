package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentInitiation;
import com.jamlapps.meridian.payment.PaymentTransaction;
import com.jamlapps.meridian.shared.Money;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PaymentInitiationAdapter implements PaymentInitiation {

    private final PaymentService service;

    PaymentInitiationAdapter(PaymentService service) {
        this.service = service;
    }

    @Override
    public PaymentTransaction initiate(UUID parentOrderId, Money amount, UUID buyerAccountId, String buyerPhoneNumber) {
        return service.initiate(parentOrderId, amount, buyerAccountId, buyerPhoneNumber);
    }
}
