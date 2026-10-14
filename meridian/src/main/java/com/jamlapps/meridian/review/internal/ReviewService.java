package com.jamlapps.meridian.review.internal;

import com.jamlapps.meridian.catalog.Product;
import com.jamlapps.meridian.catalog.Products;
import com.jamlapps.meridian.order.Orders;
import com.jamlapps.meridian.review.RatingSummary;
import com.jamlapps.meridian.review.Review;
import com.jamlapps.meridian.review.ReviewStatus;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-REV-01 to 06. */
@Service
class ReviewService {

    private final ReviewRepository reviews;
    private final Orders orders;
    private final Products products;
    private final Vendors vendors;
    private final Clock clock;

    ReviewService(ReviewRepository reviews, Orders orders, Products products, Vendors vendors, Clock clock) {
        this.reviews = reviews;
        this.orders = orders;
        this.products = products;
        this.vendors = vendors;
        this.clock = clock;
    }

    /**
     * BR-REV-01/02/06: validated in this order — self-review (BR-REV-06) before the delivered-order
     * check (BR-REV-01), because a vendor reviewing their own product is refused on that basis
     * regardless of whether they also happen to have a delivered order for it.
     */
    @Transactional
    Review submit(UUID buyerAccountId, UUID productId, int rating, @Nullable String comment) {
        if (rating < 1 || rating > 5) {
            throw ApiException.validation("must be between 1 and 5", Map.of("rating", List.of("must be between 1 and 5")));
        }

        Product product = products.findById(productId).orElseThrow(() -> ApiException.notFound("Product", productId));

        boolean isOwnProduct =
                vendors.findByOwnerAccountId(buyerAccountId).map(vendor -> vendor.id().equals(product.vendorId())).orElse(false);
        if (isOwnProduct) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A vendor may not review their own product.");
        }

        if (!orders.hasDeliveredProduct(buyerAccountId, productId)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "Only buyers with a delivered order for this product may review it.");
        }

        if (reviews.existsByBuyerAccountIdAndProductId(buyerAccountId, productId)) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This product has already been reviewed.");
        }

        ReviewEntity review = reviews.save(
                new ReviewEntity(UUID.randomUUID(), productId, product.vendorId(), buyerAccountId, rating, comment, clock.instant()));

        return review.toReview();
    }

    /** BR-REV-04: once — repo-level vendor filtering, matching {@code order.internal.OrderService.markDelivered}. */
    @Transactional
    Review respond(UUID reviewId, UUID callerAccountId, String response) {
        UUID vendorId = requireVendorId(callerAccountId);
        ReviewEntity review = reviews.findByIdAndVendorId(reviewId, vendorId).orElseThrow(() -> ApiException.notFound("Review", reviewId));

        if (review.hasVendorResponse()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "This review already has a response.");
        }

        review.respond(response, clock.instant());
        reviews.save(review);
        return review.toReview();
    }

    /** BR-REV-05: {@code reason} non-blank is validated by the controller, matching {@code AdminVendorApplicationController}'s precedent. */
    @Transactional
    Review remove(UUID reviewId, UUID adminAccountId, String reason) {
        ReviewEntity review = reviews.findById(reviewId).orElseThrow(() -> ApiException.notFound("Review", reviewId));
        review.remove(reason, adminAccountId, clock.instant());
        reviews.save(review);
        return review.toReview();
    }

    @Transactional(readOnly = true)
    List<Review> listForProduct(UUID productId) {
        return reviews.findByProductIdAndStatus(productId, ReviewStatus.ACTIVE).stream().map(ReviewEntity::toReview).toList();
    }

    /** BR-REV-03. */
    @Transactional(readOnly = true)
    RatingSummary productSummary(UUID productId) {
        Double average = reviews.averageRatingForProduct(productId);
        long count = reviews.countByProductIdAndStatus(productId, ReviewStatus.ACTIVE);
        return toSummary(average, count);
    }

    /** BR-REV-03. */
    @Transactional(readOnly = true)
    RatingSummary vendorSummary(UUID vendorId) {
        Double average = reviews.averageRatingForVendor(vendorId);
        long count = reviews.countByVendorIdAndStatus(vendorId, ReviewStatus.ACTIVE);
        return toSummary(average, count);
    }

    private static RatingSummary toSummary(@Nullable Double average, long count) {
        BigDecimal rounded = average == null ? BigDecimal.ZERO : BigDecimal.valueOf(average).setScale(2, RoundingMode.HALF_UP);
        return new RatingSummary(rounded, count);
    }

    private UUID requireVendorId(UUID callerAccountId) {
        return vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may respond to a review."))
                .id();
    }
}
