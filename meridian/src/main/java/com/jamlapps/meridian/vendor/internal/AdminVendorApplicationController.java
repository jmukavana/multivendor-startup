package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.Vendor;
import com.jamlapps.meridian.vendor.VendorApplication;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.SortSpec;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-ADM-01, BR-VND-04/06/10: the vendor approval queue and its actions.
 *
 * <p>Lives in {@code vendor.internal} rather than a separate {@code admin} module, method-secured
 * with {@code @PreAuthorize} on each endpoint — the same choice made for {@code order}'s
 * payment-confirmation endpoint in Phase 1, and for the same reason: the state transitions here are
 * {@code vendor}'s own business logic (BR-VND-04/05/06), not generic back-office plumbing, and the
 * {@code admin} module described in ADR-002 as an aggregation point over many modules' queues does
 * not exist yet. Standing up that aggregation is future work, not a precondition for this endpoint
 * being correctly guarded today.
 */
@RestController
@RequestMapping("/admin/vendor-applications")
class AdminVendorApplicationController {

    private final VendorApplicationService applications;
    private final VendorService vendors;
    private final Clock clock;

    AdminVendorApplicationController(VendorApplicationService applications, VendorService vendors, Clock clock) {
        this.applications = applications;
        this.vendors = vendors;
        this.clock = clock;
    }

    record RequestInformationRequest(@NotBlank(message = "must not be blank") String note) {}

    record RejectRequest(@NotBlank(message = "a rejection reason is required") String reason) {}

    record SuspendRequest(@NotBlank(message = "a suspension reason is required") String reason) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<VendorApplication>> queue(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int perPage) {

        var result = applications.queue(PageRequest.of(page, perPage));

        return ApiResponse.paged(
                result.getContent(),
                page,
                perPage,
                result.getTotalElements(),
                List.of(SortSpec.asc("submittedAt")),
                Map.of());
    }

    @PostMapping("/{applicationId}/under-review")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<VendorApplication> moveToUnderReview(@PathVariable UUID applicationId) {
        return ApiResponse.of(applications.moveToUnderReview(applicationId, clock.instant()));
    }

    @PostMapping("/{applicationId}/request-information")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<VendorApplication> requestInformation(
            @PathVariable UUID applicationId, @Valid @RequestBody RequestInformationRequest request) {
        return ApiResponse.of(applications.requestInformation(applicationId, request.note(), clock.instant()));
    }

    @PostMapping("/{applicationId}/approve")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<VendorApplication> approve(
            @PathVariable UUID applicationId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(applications.approve(applicationId, principal.accountId(), clock.instant()));
    }

    @PostMapping("/{applicationId}/reject")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<VendorApplication> reject(
            @PathVariable UUID applicationId,
            @Valid @RequestBody RejectRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(
                applications.reject(applicationId, principal.accountId(), request.reason(), clock.instant()));
    }

    @PostMapping("/vendors/{vendorId}/suspend")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Vendor> suspend(@PathVariable UUID vendorId, @Valid @RequestBody SuspendRequest request) {
        return ApiResponse.of(vendors.suspend(vendorId, request.reason()));
    }
}
