package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.Carrier;
import com.jamlapps.meridian.fulfilment.Delivery;
import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryZone;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BR-FUL-01/02/04/06/07, AC-FUL-01/02/04. */
@RestController
@RequestMapping("/fulfilment")
class DeliveryController {

    private final DeliveryService service;

    DeliveryController(DeliveryService service) {
        this.service = service;
    }

    record BookRequest(
            @NotNull(message = "must be provided") UUID carrierId,
            @NotNull(message = "must be provided") UUID zoneId,
            @NotNull(message = "must be provided") DeliveryMethod method) {}

    record ManualDispatchRequest(@NotBlank(message = "a tracking reference is required") String trackingReference) {}

    record MarkDeliveredRequest(@Nullable String proofOfDeliveryReference) {}

    /** Reference data a vendor needs to fill in {@link BookRequest} — plain lookups, no ownership to check. */
    @GetMapping("/carriers")
    ApiResponse<List<Carrier>> carriers() {
        return ApiResponse.of(service.listCarriers());
    }

    @GetMapping("/zones")
    ApiResponse<List<DeliveryZone>> zones() {
        return ApiResponse.of(service.listZones());
    }

    @GetMapping("/deliveries/{deliveryId}")
    ApiResponse<Delivery> get(@PathVariable UUID deliveryId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.get(deliveryId, principal.accountId()));
    }

    @GetMapping("/sub-orders/{subOrderId}/delivery")
    ApiResponse<Delivery> getBySubOrder(@PathVariable UUID subOrderId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.getBySubOrder(subOrderId, principal.accountId()));
    }

    @PostMapping("/deliveries/{deliveryId}/book")
    ApiResponse<Delivery> book(
            @PathVariable UUID deliveryId, @Valid @RequestBody BookRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.book(deliveryId, principal.accountId(), request.carrierId(), request.zoneId(), request.method()));
    }

    @PostMapping("/deliveries/{deliveryId}/dispatch")
    ApiResponse<Delivery> dispatch(@PathVariable UUID deliveryId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.dispatch(deliveryId, principal.accountId()));
    }

    /** BR-FUL-06. */
    @PostMapping("/deliveries/{deliveryId}/dispatch-manually")
    ApiResponse<Delivery> dispatchManually(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody ManualDispatchRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.dispatchManually(deliveryId, principal.accountId(), request.trackingReference()));
    }

    /** AC-FUL-02. */
    @PostMapping("/deliveries/{deliveryId}/mark-in-transit")
    ApiResponse<Delivery> markInTransit(@PathVariable UUID deliveryId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.markInTransit(deliveryId, principal.accountId()));
    }

    /** BR-FUL-06/07. */
    @PostMapping("/deliveries/{deliveryId}/mark-delivered")
    ApiResponse<Delivery> markDelivered(
            @PathVariable UUID deliveryId,
            @Valid @RequestBody MarkDeliveredRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.markDelivered(deliveryId, principal.accountId(), request.proofOfDeliveryReference()));
    }
}
