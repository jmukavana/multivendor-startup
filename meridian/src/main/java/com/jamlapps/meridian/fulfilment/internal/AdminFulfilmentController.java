package com.jamlapps.meridian.fulfilment.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.fulfilment.DeliveryRateCard;
import com.jamlapps.meridian.fulfilment.DeliveryZone;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.web.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Currency;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-FUL-08: admin configuration of delivery zones and rate cards. Lives in {@code
 * fulfilment.internal} rather than a separate {@code admin} module, method-secured with {@code
 * @PreAuthorize}, matching {@code AdminVendorApplicationController} and every other admin surface
 * this codebase has built ahead of that module existing.
 */
@RestController
@RequestMapping("/admin/fulfilment")
class AdminFulfilmentController {

    private final DeliveryService service;

    AdminFulfilmentController(DeliveryService service) {
        this.service = service;
    }

    record CreateZoneRequest(
            @NotBlank(message = "must not be blank") String name, @NotBlank(message = "must not be blank") String county) {}

    record SetRateCardRequest(
            @NotNull(message = "must be provided") UUID carrierId,
            @NotNull(message = "must be provided") UUID zoneId,
            @NotNull(message = "must be provided") DeliveryMethod method,
            @Positive(message = "must be positive") long priceMinorUnits,
            @NotBlank(message = "must not be blank") String currency) {}

    @PostMapping("/zones")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<DeliveryZone> createZone(@Valid @RequestBody CreateZoneRequest request) {
        return ApiResponse.of(service.createZone(request.name(), request.county()));
    }

    @PostMapping("/zones/{zoneId}/deactivate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Void> deactivateZone(@PathVariable UUID zoneId) {
        service.deactivateZone(zoneId);
        return ApiResponse.of(null);
    }

    /** DAT-07: always a new versioned row — see {@code DeliveryService.setRateCard}'s javadoc. */
    @PostMapping("/rate-cards")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<DeliveryRateCard> setRateCard(@Valid @RequestBody SetRateCardRequest request) {
        Money price = Money.of(request.priceMinorUnits(), Currency.getInstance(request.currency()));
        return ApiResponse.of(service.setRateCard(request.carrierId(), request.zoneId(), request.method(), price));
    }
}
