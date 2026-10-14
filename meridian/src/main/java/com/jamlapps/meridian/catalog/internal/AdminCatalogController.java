package com.jamlapps.meridian.catalog.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-ADM-05: catalog moderation tools. Lives in {@code catalog.internal}, method-secured, matching
 * every other admin surface this codebase has built ahead of the {@code admin} module's own
 * aggregation existing.
 */
@RestController
@RequestMapping("/admin/catalog")
class AdminCatalogController {

    private final CatalogService service;

    AdminCatalogController(CatalogService service) {
        this.service = service;
    }

    record ModerateDelistRequest(@NotBlank(message = "a reason is required") String reason) {}

    @PostMapping("/products/{productId}/moderate-delist")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Product> moderateDelist(
            @PathVariable UUID productId, @Valid @RequestBody ModerateDelistRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.moderateDelist(productId, principal.accountId(), request.reason()));
    }
}
