package com.jamlapps.meridian.payment;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

/**
 * BR-PAY-02: the cross-module entry point, called by {@code checkout} — the caller supplies the
 * amount rather than this module looking an order up itself. {@code payment} depends on no other
 * module (see its {@code package-info.java}); trusting the caller for the amount is what keeps
 * that true, and the caller here is {@code checkout}, an internal orchestrator, not a buyer-supplied HTTP parameter that would need independent verification.
 */
public interface PaymentInitiation {
    PaymentTransaction initiate(UUID parentOrderId, Money amount, UUID buyerAccountId, String buyerPhoneNumber);
}
