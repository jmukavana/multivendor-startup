package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.iam.ImpersonationRecord;
import com.jamlapps.meridian.iam.IssuedImpersonationSession;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-ADM-08: both halves of impersonation in one controller — the admin-facing action that starts
 * it and the vendor-facing read that makes it visible — even though their paths sit under two
 * different prefixes ({@code /admin/vendors/...}, {@code /vendors/...}), the same split every
 * other route in this codebase uses between an admin action and the account it acts on. Kept
 * together here because they are one feature, not two, and reviewing BR-ADM-08 as a whole means
 * reading both ends of it in one file.
 *
 * <p>Method-secured with {@code @PreAuthorize} on the admin action, matching {@code
 * AdminVendorApplicationController}'s reasoning: this is real, security-relevant state, not
 * generic back-office plumbing, and there is no case for it waiting on a future {@code admin}
 * aggregation module.
 */
@RestController
class VendorImpersonationController {

    private final VendorImpersonationService impersonation;

    VendorImpersonationController(VendorImpersonationService impersonation) {
        this.impersonation = impersonation;
    }

    record ImpersonateRequest(@NotBlank(message = "a reason is required") String reason) {}

    @PostMapping("/admin/vendors/{vendorId}/impersonate")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<IssuedImpersonationSession> impersonate(
            @PathVariable UUID vendorId,
            @Valid @RequestBody ImpersonateRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(impersonation.impersonate(vendorId, principal.accountId(), request.reason()));
    }

    @GetMapping("/vendors/{vendorId}/impersonation-log")
    ApiResponse<List<ImpersonationRecord>> impersonationLog(
            @PathVariable UUID vendorId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(impersonation.history(vendorId, principal.accountId()));
    }
}
