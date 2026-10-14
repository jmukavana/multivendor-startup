package com.jamlapps.meridian.checkout.internal;

import com.jamlapps.meridian.fulfilment.DeliveryMethod;
import com.jamlapps.meridian.order.ParentOrder;
import com.jamlapps.meridian.order.RequestOrigin;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.ClientAddress;
import com.jamlapps.meridian.web.ErrorCode;
import com.jamlapps.meridian.web.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-CHK-01/02/04/09, UC-03: the real multi-vendor checkout entry point superseding Phase 1's
 * direct {@code POST /orders}.
 */
@RestController
@RequestMapping("/checkout")
class CheckoutController {

    private final CheckoutService service;
    private final RateLimiter rateLimiter;

    CheckoutController(CheckoutService service, RateLimiter rateLimiter) {
        this.service = service;
        this.rateLimiter = rateLimiter;
    }

    record CartLineRequest(
            @NotNull(message = "must be provided") UUID productId,
            @Min(value = 1, message = "must be at least 1") int quantity) {}

    record CheckoutRequest(
            @NotEmpty(message = "the cart must contain at least one item") List<CartLineRequest> lines,
            @NotBlank(message = "must not be blank") String buyerPhoneNumber,
            /** BR-CHK-05/06: the destination for the whole order. */
            @NotNull(message = "must be provided") UUID deliveryZoneId,
            /** BR-CHK-05: door or pickup point, per vendor id; a vendor absent from this map defaults to DOOR. */
            @Nullable Map<UUID, DeliveryMethod> deliveryMethodByVendor) {

        CheckoutRequest {
            deliveryMethodByVendor = deliveryMethodByVendor == null ? Map.of() : Map.copyOf(deliveryMethodByVendor);
        }
    }

    /** BR-SEC-01: "checkout initiation", by account and by source address. */
    @PostMapping
    ApiResponse<ParentOrder> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal,
            HttpServletRequest httpRequest) {
        boolean accountOk =
                rateLimiter.tryConsume(RateLimiter.SCOPE_CHECKOUT_INITIATION_ACCOUNT, principal.accountId().toString());
        String sourceAddress = ClientAddress.of(httpRequest);
        boolean sourceOk = sourceAddress == null
                || rateLimiter.tryConsume(RateLimiter.SCOPE_CHECKOUT_INITIATION_SOURCE, sourceAddress);
        if (!accountOk || !sourceOk) {
            throw new ApiException(ErrorCode.RATE_LIMITED, "Too many checkout attempts. Try again shortly.");
        }

        List<CheckoutService.CartLine> lines = request.lines().stream()
                .map(line -> new CheckoutService.CartLine(line.productId(), line.quantity()))
                .toList();

        // BR-SEC-03: the same source address the rate limit above already resolved.
        RequestOrigin origin = new RequestOrigin(sourceAddress, httpRequest.getHeader("User-Agent"));

        return ApiResponse.of(service.checkout(
                principal.accountId(),
                lines,
                request.buyerPhoneNumber(),
                request.deliveryZoneId(),
                request.deliveryMethodByVendor(),
                origin));
    }
}
