package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.SubOrder;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.SortSpec;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-ORD-09, BR-ORD-10.
 *
 * <p>Two capabilities are deliberately not exposed here, both retired in favour of a real trigger
 * once the module that owns it existed:
 *
 * <ul>
 *   <li><b>Creation</b> — {@code POST /orders} was Phase 1's direct, single-product
 *       walking-skeleton stand-in. Phase 4's real entry point is
 *       {@code checkout.internal.CheckoutController}, which revalidates a multi-vendor cart and
 *       reserves stock before calling {@link com.jamlapps.meridian.order.OrderCreation}.
 *   <li><b>Payment confirmation</b> — {@code POST /orders/{id}/mark-payment-confirmed} was a
 *       finance-admin-gated stand-in for BR-PAY-04's "confirmed only upon a verified provider
 *       callback". Phase 3's real trigger is {@code payment.events.PaymentConfirmed}, consumed by
 *       {@code PaymentConfirmationListener} — see {@code OrderService.markPaymentConfirmed}'s
 *       javadoc.
 * </ul>
 *
 * <p>Neither capability keeps a manual HTTP path alongside its real trigger: a second way to reach
 * the same effect is exactly the surface these retirements exist to remove.
 *
 * <p><b>{@code mark-delivered} is deliberately still here</b>, even though {@code fulfilment}
 * (Phase 7) now exists and has its own, much richer delivery lifecycle. It was not retired: {@code
 * fulfilment}'s allowed dependency on {@code order::events} is one-directional (ADR-002) — {@code
 * order} depending back on {@code fulfilment::events} to drive this transition would be a module
 * cycle, and {@code ModularityTests} refuses it. See {@code OrderStatus}'s javadoc for the full
 * reasoning; this endpoint and {@code fulfilment}'s own delivery-completion endpoint are two
 * genuinely separate triggers for two different things, not one retired in favour of the other.
 */
@RestController
@RequestMapping("/orders")
class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping("/{orderId}")
    ApiResponse<ParentOrder> get(@PathVariable UUID orderId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.get(orderId, principal.accountId()));
    }

    @GetMapping
    ApiResponse<List<ParentOrder>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @AuthenticationPrincipal AuthenticatedCaller principal) {

        var result = service.listForBuyer(principal.accountId(), PageRequest.of(page, perPage));

        return ApiResponse.paged(
                result.getContent(),
                page,
                perPage,
                result.getTotalElements(),
                List.of(SortSpec.desc("createdAt")),
                Map.of());
    }

    /** The vendor-dashboard equivalent of the buyer {@link #list} above — one vendor's own order queue. */
    @GetMapping("/vendor/sub-orders")
    ApiResponse<List<SubOrder>> listForVendor(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int perPage,
            @AuthenticationPrincipal AuthenticatedCaller principal) {

        var result = service.listForVendor(principal.accountId(), PageRequest.of(page, perPage));

        return ApiResponse.paged(
                result.getContent(),
                page,
                perPage,
                result.getTotalElements(),
                List.of(SortSpec.desc("createdAt")),
                Map.of());
    }

    /** See {@code OrderService.markDelivered}'s javadoc and this class's own doc on why this stays, unretired, alongside {@code fulfilment}. */
    @PostMapping("/sub-orders/{subOrderId}/mark-delivered")
    ApiResponse<Void> markDelivered(@PathVariable UUID subOrderId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        service.markDelivered(subOrderId, principal.accountId());
        return ApiResponse.of(null);
    }

    /** BR-ORD-05. Auto-cancellation past the window is {@code AcceptanceWindowSweeper}'s job, not an HTTP path. */
    @PostMapping("/sub-orders/{subOrderId}/accept")
    ApiResponse<SubOrder> accept(@PathVariable UUID subOrderId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.accept(subOrderId, principal.accountId()));
    }

    /** BR-ORD-13/14. */
    @PostMapping("/sub-orders/{subOrderId}/cancel")
    ApiResponse<SubOrder> cancel(@PathVariable UUID subOrderId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.cancel(subOrderId, principal.accountId()));
    }
}
