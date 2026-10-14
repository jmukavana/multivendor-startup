package com.jamlapps.meridian.payment;

import java.util.Optional;

/** How other modules ask about payment transactions. Read-only, matching {@code iam.Accounts}' reasoning. */
public interface Payments {

    /** BR-ADM-02: the "by payment reference" half of admin order search. */
    Optional<PaymentTransaction> findByProviderTransactionReference(String providerTransactionReference);
}
