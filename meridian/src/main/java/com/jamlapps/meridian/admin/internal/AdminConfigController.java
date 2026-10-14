package com.jamlapps.meridian.admin.internal;

import com.jamlapps.meridian.pricing.CommissionRate;
import com.jamlapps.meridian.pricing.HoldbackRate;
import com.jamlapps.meridian.pricing.PricingAdministration;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-ADM-06: configuration management — the commission-rate and holdback-rate pieces. Delivery
 * zones are already admin-configurable via {@code fulfilment.internal.AdminFulfilmentController},
 * not duplicated here; settlement cycle lives directly in {@code settlement.internal
 * .SettlementController} instead, matching that module's own admin-surface reasoning — see {@code
 * admin.package-info} for why.
 */
@RestController
@RequestMapping("/admin/config")
class AdminConfigController {

    private final PricingAdministration pricing;
    private final Clock clock;

    AdminConfigController(PricingAdministration pricing, Clock clock) {
        this.pricing = pricing;
        this.clock = clock;
    }

    record CreateCommissionConfigurationRequest(
            @NotNull(message = "must be provided") @DecimalMin(value = "0", message = "must be at least 0")
                    @DecimalMax(value = "1", inclusive = false, message = "must be less than 1")
                    BigDecimal rate,
            @Nullable Instant effectiveFrom) {}

    @PostMapping("/commission-rate")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<CommissionRate> createCommissionRate(
            @Valid @RequestBody CreateCommissionConfigurationRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        Instant effectiveFrom = request.effectiveFrom() != null ? request.effectiveFrom() : clock.instant();
        return ApiResponse.of(pricing.createCommissionConfiguration(request.rate(), effectiveFrom, principal.accountId()));
    }

    record CreateHoldbackConfigurationRequest(
            @NotNull(message = "must be provided") @DecimalMin(value = "0", message = "must be at least 0")
                    @DecimalMax(value = "1", inclusive = false, message = "must be less than 1")
                    BigDecimal rate,
            @Nullable Instant effectiveFrom) {}

    @PostMapping("/holdback-rate")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<HoldbackRate> createHoldbackRate(
            @Valid @RequestBody CreateHoldbackConfigurationRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        Instant effectiveFrom = request.effectiveFrom() != null ? request.effectiveFrom() : clock.instant();
        return ApiResponse.of(pricing.createHoldbackConfiguration(request.rate(), effectiveFrom, principal.accountId()));
    }
}
