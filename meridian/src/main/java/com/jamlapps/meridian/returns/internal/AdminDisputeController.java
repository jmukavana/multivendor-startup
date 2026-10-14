package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.Dispute;
import com.jamlapps.meridian.returns.DisputeFavor;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * BR-RTN-06/07: the admin dispute queue and its resolution. Lives in {@code returns.internal}
 * rather than a separate {@code admin} module, method-secured with {@code @PreAuthorize} — the same
 * choice {@code AdminVendorApplicationController} and {@code SettlementController} already made,
 * for the same reason: the {@code admin} aggregation module ADR-002 describes does not exist yet.
 *
 * <p>BR-RTN-06's "full order, payment, and communication history" is reduced here to what {@link
 * Dispute} and its {@code Return} already carry — the return's reason, description, images and
 * requested lines. There is no messaging/chat system anywhere in this codebase to supply
 * "communication history" from; that is a pre-existing gap this endpoint surfaces rather than one
 * it introduces.
 */
@RestController
@RequestMapping("/admin/returns/disputes")
class AdminDisputeController {

    private final ReturnService service;

    AdminDisputeController(ReturnService service) {
        this.service = service;
    }

    record ResolveDisputeRequest(
            @NotNull(message = "must be provided") DisputeFavor favor,
            @NotBlank(message = "a resolution reason is required") String reason) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<List<Dispute>> queue() {
        return ApiResponse.of(service.disputeQueue());
    }

    /** AC-RTN-04: a blank reason is refused by {@code @NotBlank} before this method is even entered. */
    @PostMapping("/{disputeId}/resolve")
    @PreAuthorize("hasAnyRole('FINANCE_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Dispute> resolve(
            @PathVariable UUID disputeId,
            @Valid @RequestBody ResolveDisputeRequest request,
            @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.resolveDispute(disputeId, principal.accountId(), request.favor(), request.reason()));
    }
}
