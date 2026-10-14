package com.jamlapps.meridian.review.internal;

import com.jamlapps.meridian.review.RatingSummary;
import com.jamlapps.meridian.review.Review;
import com.jamlapps.meridian.web.ApiResponse;
import com.jamlapps.meridian.web.AuthenticatedCaller;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** BR-REV-01 to 04. */
@RestController
@RequestMapping
class ReviewController {

    private final ReviewService service;

    ReviewController(ReviewService service) {
        this.service = service;
    }

    record SubmitReviewRequest(
            @NotNull(message = "must be provided") UUID productId,
            @Min(value = 1, message = "must be between 1 and 5") @Max(value = 5, message = "must be between 1 and 5") int rating,
            @Nullable String comment) {}

    record RespondRequest(@NotBlank(message = "a response is required") String response) {}

    @PostMapping("/reviews")
    ApiResponse<Review> submit(@Valid @RequestBody SubmitReviewRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.submit(principal.accountId(), request.productId(), request.rating(), request.comment()));
    }

    /** BR-REV-04. */
    @PostMapping("/reviews/{reviewId}/response")
    ApiResponse<Review> respond(
            @PathVariable UUID reviewId, @Valid @RequestBody RespondRequest request, @AuthenticationPrincipal AuthenticatedCaller principal) {
        return ApiResponse.of(service.respond(reviewId, principal.accountId(), request.response()));
    }

    /** BR-IAM-08-style open listing, matching {@code catalog}'s public product listing — no authentication required. */
    @GetMapping("/catalog/products/{productId}/reviews")
    ApiResponse<List<Review>> forProduct(@PathVariable UUID productId) {
        return ApiResponse.of(service.listForProduct(productId));
    }

    /** BR-REV-03. */
    @GetMapping("/catalog/products/{productId}/reviews/summary")
    ApiResponse<RatingSummary> productSummary(@PathVariable UUID productId) {
        return ApiResponse.of(service.productSummary(productId));
    }

    /** BR-REV-03, BR-VND-11's storefront rating — not yet consumed cross-module, but real and independently queryable now. */
    @GetMapping("/vendors/{vendorId}/reviews/summary")
    ApiResponse<RatingSummary> vendorSummary(@PathVariable UUID vendorId) {
        return ApiResponse.of(service.vendorSummary(vendorId));
    }
}
