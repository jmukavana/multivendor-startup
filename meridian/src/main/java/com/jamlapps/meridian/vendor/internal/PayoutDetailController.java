package com.jamlapps.meridian.vendor.internal;

import com.jamlapps.meridian.vendor.internal.VendorMembershipService.Relationship;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import com.jamlapps.meridian.web.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BR-VND-08/09, BR-IAM-04, AC-VND-03. A caller unrelated to the vendor gets {@code NOT_FOUND}
 * (matching the cross-vendor pattern used elsewhere — see {@code OrderService.get}); a caller who
 * is that vendor's staff, and only staff, gets {@code FORBIDDEN}, per BR-IAM-04's specific wording.
 */
@RestController
@RequestMapping("/vendors/{vendorId}")
class PayoutDetailController {

    private final PayoutDetailService payoutDetails;
    private final VendorMembershipService membership;

    PayoutDetailController(PayoutDetailService payoutDetails, VendorMembershipService membership) {
        this.payoutDetails = payoutDetails;
        this.membership = membership;
    }

    record ChangePayoutDetailRequest(
            @NotNull(message = "must be provided") PayoutDetailEntity.Method method,
            @NotBlank(message = "must not be blank") String destination) {}

    record AddStaffRequest(@NotNull(message = "must be provided") UUID accountId) {}

    @GetMapping("/payout-details")
    ApiResponse<PayoutDetailService.PayoutDetailView> view(
            @PathVariable UUID vendorId, @AuthenticationPrincipal AuthenticatedCaller principal) {
        Relationship relationship = requireRelated(vendorId, principal.accountId());
        return ApiResponse.of(
                payoutDetails.view(vendorId, principal.accountId(), relationship == Relationship.STAFF));
    }

    @PutMapping("/payout-details")
    ApiResponse<Void> change(
            @PathVariable UUID vendorId,
            @Valid @RequestBody ChangePayoutDetailRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        Relationship relationship = requireRelated(vendorId, principal.accountId());
        payoutDetails.change(
                vendorId, principal.accountId(), relationship == Relationship.STAFF, request.method(), request.destination());
        return ApiResponse.of(null);
    }

    @PostMapping("/staff")
    ApiResponse<Void> addStaff(
            @PathVariable UUID vendorId,
            @Valid @RequestBody AddStaffRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        if (membership.relationshipOf(vendorId, principal.accountId()) != Relationship.OWNER) {
            throw new ApiException(ErrorCode.FORBIDDEN, "Only the vendor owner may add staff.");
        }
        membership.addStaff(vendorId, request.accountId());
        return ApiResponse.of(null);
    }

    private Relationship requireRelated(UUID vendorId, UUID accountId) {
        Relationship relationship = membership.relationshipOf(vendorId, accountId);
        if (relationship == Relationship.UNRELATED) {
            throw ApiException.notFound("Vendor", vendorId);
        }
        return relationship;
    }
}
