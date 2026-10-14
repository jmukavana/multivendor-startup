package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentTransaction;
import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-ADM-03: the payment exception queue. Lives in {@code payment.internal}, method-secured,
 * matching every other admin surface this codebase has built ahead of the {@code admin} module's
 * own aggregation existing.
 */
@RestController
@RequestMapping("/admin/payments/exceptions")
class AdminPaymentController {

    private final PaymentService service;

    AdminPaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<PaymentTransaction>> exceptions() {
        return ApiResponse.of(service.exceptionTransactions());
    }
}
