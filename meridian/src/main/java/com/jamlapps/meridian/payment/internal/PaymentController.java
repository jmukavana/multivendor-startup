package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentTransaction;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
class PaymentController {

    private final PaymentService service;

    PaymentController(PaymentService service) {
        this.service = service;
    }

    @GetMapping("/{paymentTransactionId}")
    ApiResponse<PaymentTransaction> get(
            @PathVariable UUID paymentTransactionId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.get(paymentTransactionId, principal.accountId()));
    }

    /**
     * BR-PAY-04/05: the provider's webhook. Permitted without authentication in
     * {@code SecurityConfiguration} — a real aggregator calling this has no session token, only its
     * own (unimplemented pending OPEN-03) signature scheme. Named to match how a test or a future
     * real integration would reach it; production hardening (signature verification, IP
     * allow-listing) is not implemented — there is no real provider to verify against yet.
     */
    @PostMapping("/callback")
    ApiResponse<PaymentTransaction> callback(@Valid @RequestBody CallbackRequest request) {
        return ApiResponse.of(service.handleCallback(
                request.gatewayReference(), request.providerTransactionReference(), request.success()));
    }

    record CallbackRequest(
            @NotBlank(message = "must not be blank") String gatewayReference,
            @NotBlank(message = "must not be blank") String providerTransactionReference,
            boolean success) {}
}
