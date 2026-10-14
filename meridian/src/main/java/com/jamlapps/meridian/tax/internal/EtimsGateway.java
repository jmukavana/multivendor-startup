package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.shared.Money;

/**
 * ADR-006, INT-02: KRA's eTIMS, behind an interface in this module's own vocabulary — matching
 * {@code payment.internal.PaymentGateway}'s reasoning exactly. Transmits either document type;
 * eTIMS' own model treats a credit note as a signed-invoice variant, not a separate API shape.
 */
interface EtimsGateway {

    /** REG-04/07: transmits a tax invoice, returning KRA's acknowledgement reference. */
    GatewayTransmission transmitInvoice(String invoiceNumber, Money amount);

    /** REG-08: transmits a credit note, referencing the invoice it corrects. */
    GatewayTransmission transmitCreditNote(String invoiceNumber, Money amount);

    record GatewayTransmission(boolean accepted, String acknowledgementReference) {}
}
