package com.jamlapps.meridian.integrity.internal;

import com.jamlapps.meridian.integrity.Restriction;
import com.jamlapps.meridian.integrity.RestrictionStatus;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-SEC-07/08, AC-SEC-02: the admin exception queue for automated restrictions, and the one place
 * a restriction is ever lifted. Lives in {@code integrity.internal}, method-secured, matching every
 * other admin surface this codebase has built ahead of an {@code admin} aggregation module
 * existing.
 */
@RestController
@RequestMapping("/admin/integrity/restrictions")
class AdminIntegrityController {

    private final RestrictionService service;

    AdminIntegrityController(RestrictionService service) {
        this.service = service;
    }

    record LiftRequest(@NotBlank(message = "a reason is required") String reason) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<Restriction>> active() {
        return ApiResponse.of(service.list(RestrictionStatus.ACTIVE));
    }

    @GetMapping("/lifted")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<Restriction>> lifted() {
        return ApiResponse.of(service.list(RestrictionStatus.LIFTED));
    }

    /** AC-SEC-02: lifted in full — there is no partial or scoped lift. */
    @PostMapping("/{restrictionId}/lift")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Restriction> lift(
            @PathVariable UUID restrictionId, @RequestBody LiftRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.lift(restrictionId, principal.accountId(), request.reason()));
    }
}
