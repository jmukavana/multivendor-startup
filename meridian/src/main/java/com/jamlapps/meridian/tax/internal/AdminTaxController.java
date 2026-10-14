package com.jamlapps.meridian.tax.internal;

import com.jamlapps.meridian.tax.CreditNote;
import com.jamlapps.meridian.tax.TaxInvoice;
import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REG-06: the admin exception queue for eTIMS transmission failures. Lives in {@code
 * tax.internal}, method-secured, matching every other admin surface this codebase has built ahead
 * of an {@code admin} aggregation module existing.
 */
@RestController
@RequestMapping("/admin/tax")
class AdminTaxController {

    private final TaxService service;

    AdminTaxController(TaxService service) {
        this.service = service;
    }

    /** REG-07. */
    @GetMapping("/orders/{parentOrderId}/invoice")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<TaxInvoice> invoiceForOrder(@PathVariable UUID parentOrderId) {
        return ApiResponse.of(service.getInvoiceByOrder(parentOrderId));
    }

    /** REG-07/08. */
    @GetMapping("/orders/{parentOrderId}/credit-notes")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<CreditNote>> creditNotesForOrder(@PathVariable UUID parentOrderId) {
        return ApiResponse.of(service.getCreditNotesByOrder(parentOrderId));
    }

    @GetMapping("/invoices/exceptions")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<TaxInvoice>> exceptionInvoices() {
        return ApiResponse.of(service.exceptionInvoices());
    }

    @GetMapping("/credit-notes/exceptions")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<CreditNote>> exceptionCreditNotes() {
        return ApiResponse.of(service.exceptionCreditNotes());
    }
}
