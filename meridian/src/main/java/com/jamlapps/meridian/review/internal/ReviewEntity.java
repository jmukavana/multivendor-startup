package com.jamlapps.meridian.review.internal;

import com.jamlapps.meridian.review.Review;
import com.jamlapps.meridian.review.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** BR-REV-01 to 06. */
@Entity
@Table(name = "review")
class ReviewEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "buyer_account_id", nullable = false)
    private UUID buyerAccountId;

    @Column(nullable = false)
    private int rating;

    @Column
    private @Nullable String comment;

    @Column(name = "vendor_response")
    private @Nullable String vendorResponse;

    @Column(name = "vendor_response_at")
    private @Nullable Instant vendorResponseAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @Column(name = "removal_reason")
    private @Nullable String removalReason;

    @Column(name = "removed_by_account_id")
    private @Nullable UUID removedByAccountId;

    @Column(name = "removed_at")
    private @Nullable Instant removedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReviewEntity() {
        // for JPA
    }

    ReviewEntity(UUID id, UUID productId, UUID vendorId, UUID buyerAccountId, int rating, @Nullable String comment, Instant createdAt) {
        this.id = id;
        this.productId = productId;
        this.vendorId = vendorId;
        this.buyerAccountId = buyerAccountId;
        this.rating = rating;
        this.comment = comment;
        this.status = ReviewStatus.ACTIVE;
        this.createdAt = createdAt;
    }

    /** BR-REV-04: only once — caller checks {@link #hasVendorResponse()} first. */
    void respond(String response, Instant now) {
        this.vendorResponse = response;
        this.vendorResponseAt = now;
    }

    boolean hasVendorResponse() {
        return vendorResponse != null;
    }

    /** BR-REV-05. */
    void remove(String reason, UUID removedByAccountId, Instant now) {
        this.status = ReviewStatus.REMOVED;
        this.removalReason = reason;
        this.removedByAccountId = removedByAccountId;
        this.removedAt = now;
    }

    UUID id() {
        return id;
    }

    UUID vendorId() {
        return vendorId;
    }

    UUID buyerAccountId() {
        return buyerAccountId;
    }

    ReviewStatus status() {
        return status;
    }

    Review toReview() {
        return new Review(id, productId, vendorId, buyerAccountId, rating, comment, vendorResponse, status, removalReason, createdAt);
    }
}
