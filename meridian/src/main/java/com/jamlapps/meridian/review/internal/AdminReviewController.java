package com.jamlapps.meridian.review.internal;

import com.jamlapps.meridian.review.Review;
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
 * BR-REV-05: content-policy removal, with reason logged. Lives in {@code review.internal} rather
 * than a separate {@code admin} module, method-secured — the same choice every other admin
 * surface in this codebase has made ahead of that module existing.
 */
@RestController
@RequestMapping("/admin/reviews")
class AdminReviewController {

    private final ReviewService service;

    AdminReviewController(ReviewService service) {
        this.service = service;
    }

    record RemoveReviewRequest(@NotBlank(message = "a removal reason is required") String reason) {}

    @PostMapping("/{reviewId}/remove")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'SUPER_ADMIN')")
    ApiResponse<Review> remove(
            @PathVariable UUID reviewId, @Valid @RequestBody RemoveReviewRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.remove(reviewId, principal.accountId(), request.reason()));
    }
}
