package com.jamlapps.meridian.settlement.internal;

import com.jamlapps.meridian.settlement.SettlementLineStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface SettlementLineRepository extends JpaRepository<SettlementLineEntity, UUID> {

    List<SettlementLineEntity> findBySettlementRunId(UUID settlementRunId);

    /** BR-STL-07: the retry sweep scans exactly this, matching {@code stock_reservation}'s equivalent index. */
    List<SettlementLineEntity> findByStatus(SettlementLineStatus status);

    /** BR-ADM-06: the vendor's most recent successful disbursement, if any — what the settlement-cycle exclusion measures from. */
    Optional<SettlementLineEntity> findTopByVendorIdAndStatusOrderByResolvedAtDesc(UUID vendorId, SettlementLineStatus status);

    /**
     * BR-STL-12: claims a line for disbursement — an atomic UPDATE with the precondition in its own
     * WHERE clause, not read-then-write, matching {@code PaymentTransactionRepository.tryConfirm}
     * exactly and for the same reason: two callers racing to disburse the same line (a retry sweep
     * tick overlapping an in-flight manual retry, say) can only have one of them actually claim it.
     *
     * @return 1 if this call claimed the line, 0 if it was already DISBURSING, DISBURSED, or otherwise not claimable
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SettlementLineEntity l set l.status = com.jamlapps.meridian.settlement.SettlementLineStatus.DISBURSING "
            + "where l.id = :lineId "
            + "and l.status in (com.jamlapps.meridian.settlement.SettlementLineStatus.PENDING, "
            + "com.jamlapps.meridian.settlement.SettlementLineStatus.FAILED)")
    int tryStartDisbursing(UUID lineId);
}
