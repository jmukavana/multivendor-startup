package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.returns.ReturnStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ReturnRepository extends JpaRepository<ReturnEntity, UUID> {

    List<ReturnEntity> findByBuyerAccountIdOrderByRequestedAtDesc(UUID buyerAccountId);

    /** Repo-level ownership filtering for the vendor's own view, matching {@code order.internal.SubOrderRepository.findByIdAndVendorId}. */
    Optional<ReturnEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    /** BR-RTN-03: the timeout sweep scans exactly this. */
    List<ReturnEntity> findByStatusAndRequestedAtBefore(ReturnStatus status, Instant cutoff);

    /** Guards against a second concurrent request on the same sub-order — see {@code ReturnService.request}'s comment. */
    Optional<ReturnEntity> findBySubOrderIdAndStatus(UUID subOrderId, ReturnStatus status);

    /** BR-SEC-04: {@code BuyerReturnActivity.returnStats}'s query — see that interface's javadoc for what counts as disputed. */
    @Query("select new com.jamlapps.meridian.returns.internal.BuyerReturnRow(r.buyerAccountId, count(r), "
            + "sum(case when r.status = com.jamlapps.meridian.returns.ReturnStatus.ESCALATED "
            + "or r.status = com.jamlapps.meridian.returns.ReturnStatus.RESOLVED_VENDOR then 1L else 0L end)) "
            + "from ReturnEntity r group by r.buyerAccountId")
    List<BuyerReturnRow> returnStatsByBuyer();
}
