package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.web.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-SEC-06: the admin-visible surface for refused reservation attempts. Lives in {@code
 * inventory.internal}, method-secured, matching every other admin surface this codebase has built
 * ahead of an {@code admin} aggregation module existing.
 */
@RestController
@RequestMapping("/admin/inventory/oversell-attempts")
class AdminInventoryController {

    private final OversellAttemptRepository oversellAttempts;

    AdminInventoryController(OversellAttemptRepository oversellAttempts) {
        this.oversellAttempts = oversellAttempts;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<OversellAttemptView>> list() {
        return ApiResponse.of(oversellAttempts.findAllByOrderByOccurredAtDesc().stream().map(OversellAttemptEntity::toView).toList());
    }
}
