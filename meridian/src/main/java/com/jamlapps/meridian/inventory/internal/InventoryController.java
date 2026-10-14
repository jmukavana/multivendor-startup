package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BR-INV-01, AC-INV-04. Vendor-scoped: ownership is enforced in {@code StockManagementService}. */
@RestController
@RequestMapping("/inventory/products/{productId}")
class InventoryController {

    private final StockManagementService service;

    InventoryController(StockManagementService service) {
        this.service = service;
    }

    record SetStockRequest(@Min(value = 0, message = "must not be negative") int totalQuantity) {}

    record StockView(int totalQuantity, int availableQuantity) {}

    @PutMapping("/stock")
    ApiResponse<StockView> setStock(
            @PathVariable UUID productId,
            @Valid @RequestBody SetStockRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        service.setTotalQuantity(productId, principal.accountId(), request.totalQuantity());
        return ApiResponse.of(new StockView(
                service.totalQuantity(productId), service.availableQuantity(productId)));
    }

    @GetMapping("/stock")
    ApiResponse<StockView> getStock(
            @PathVariable UUID productId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        // Reuses movementHistory's ownership check by calling it first would be wasteful; the
        // quantity queries are read-only and unauthenticated in themselves, but this endpoint is
        // vendor management, not a public stock count, so ownership still gates it here.
        service.movementHistory(productId, principal.accountId());
        return ApiResponse.of(new StockView(service.totalQuantity(productId), service.availableQuantity(productId)));
    }

    @GetMapping("/movements")
    ApiResponse<List<StockMovementView>> movements(
            @PathVariable UUID productId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        var history = service.movementHistory(productId, principal.accountId());
        return ApiResponse.of(history.stream()
                .map(m -> new StockMovementView(m.quantityDelta(), m.reason(), m.referenceId(), m.occurredAt()))
                .toList());
    }
}
