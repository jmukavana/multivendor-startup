package com.jamlapps.meridian.review.internal;

import com.jamlapps.meridian.review.ReviewStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ReviewRepository extends JpaRepository<ReviewEntity, UUID> {

    boolean existsByBuyerAccountIdAndProductId(UUID buyerAccountId, UUID productId);

    List<ReviewEntity> findByProductIdAndStatus(UUID productId, ReviewStatus status);

    /** Repo-level vendor-ownership filtering, matching {@code order.internal.SubOrderRepository.findByIdAndVendorId}. */
    Optional<ReviewEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    long countByProductIdAndStatus(UUID productId, ReviewStatus status);

    long countByVendorIdAndStatus(UUID vendorId, ReviewStatus status);

    @Query("select avg(r.rating) from ReviewEntity r where r.productId = :productId and r.status = com.jamlapps.meridian.review.ReviewStatus.ACTIVE")
    Double averageRatingForProduct(UUID productId);

    @Query("select avg(r.rating) from ReviewEntity r where r.vendorId = :vendorId and r.status = com.jamlapps.meridian.review.ReviewStatus.ACTIVE")
    Double averageRatingForVendor(UUID vendorId);
}
