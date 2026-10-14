package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentTransaction;
import com.jamlapps.meridian.payment.Payments;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class PaymentsAdapter implements Payments {

    private final PaymentTransactionRepository transactions;

    PaymentsAdapter(PaymentTransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PaymentTransaction> findByProviderTransactionReference(String providerTransactionReference) {
        return transactions.findByProviderTransactionReference(providerTransactionReference).map(PaymentTransactionEntity::toPaymentTransaction);
    }
}
