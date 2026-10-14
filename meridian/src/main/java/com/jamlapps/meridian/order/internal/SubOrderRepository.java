package com.jamlapps.meridian.order.internal;

import com.jamlapps.meridian.order.OrderStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SubOrderRepository extends JpaRepository<SubOrderEntity, UUID> {

    // findById(UUID) already comes from JpaRepository; no need to redeclare it here.

    /**
     * Repo-level vendor-ownership filtering, matching {@code catalog.internal
     * .ProductRepository.findByIdAndVendorId} exactly: a caller who does not own this sub-order's
     * vendor gets an empty result, not a fetch-then-check, so the WHERE clause is the isolation
     * boundary rather than application code that could be written wrong.
     */
    Optional<SubOrderEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    /** A vendor's own order queue — {@code OrderService.listForVendor}'s query. */
    Page<SubOrderEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId, Pageable pageable);

    /** BR-REV-01: {@code Orders.hasDeliveredProduct}'s query. */
    @Query("select count(l) > 0 from SubOrderEntity s join s.lines l "
            + "where s.parentOrder.buyerAccountId = :buyerAccountId "
            + "and s.status = com.jamlapps.meridian.order.OrderStatus.DELIVERED "
            + "and l.productId = :productId")
    boolean existsDeliveredLineForBuyerAndProduct(UUID buyerAccountId, UUID productId);

    /** BR-ORD-05: {@code AcceptanceWindowSweeper}'s candidates — still CONFIRMED, past their acceptance deadline. */
    List<SubOrderEntity> findByStatusAndAcceptanceDeadlineBefore(OrderStatus status, Instant cutoff);

    /** BR-SEC-04: {@code BuyerOrderActivity.cancellationStats}'s query — every buyer with at least one sub-order. */
    @Query("select new com.jamlapps.meridian.order.internal.BuyerCancellationRow(s.parentOrder.buyerAccountId, "
            + "count(s), sum(case when s.status = com.jamlapps.meridian.order.OrderStatus.CANCELLED then 1L else 0L end)) "
            + "from SubOrderEntity s group by s.parentOrder.buyerAccountId")
    List<BuyerCancellationRow> cancellationStatsByBuyer();
}
