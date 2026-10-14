package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface PaymentTransactionRepository extends JpaRepository<PaymentTransactionEntity, UUID> {

    Optional<PaymentTransactionEntity> findByGatewayReference(String gatewayReference);

    /** BR-PAY-11: resolves the transaction a refund traces back to. One payment per parent order in this walking skeleton — see {@code CheckoutService}. */
    Optional<PaymentTransactionEntity> findByParentOrderId(UUID parentOrderId);

    /** BR-PAY-05: the idempotency lookup — a replayed callback resolves to this, not a new row. */
    Optional<PaymentTransactionEntity> findByProviderTransactionReference(String providerTransactionReference);

    /**
     * BR-PAY-05: the concurrency-safety mechanism, matching {@code inventory.StockRepository}'s
     * {@code tryReserve} — an atomic UPDATE with the precondition in its own WHERE clause, not a
     * read-then-write. Two callbacks racing for the same transaction (a provider's own retry
     * arriving twice, near-simultaneously) can only have one of them actually transition the row;
     * the other affects zero rows and the service treats that as "already handled", not an error.
     *
     * @return 1 if this call performed the transition, 0 if the transaction was already resolved
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PaymentTransactionEntity p set p.status = com.jamlapps.meridian.payment.PaymentStatus.CONFIRMED, "
            + "p.providerTransactionReference = :providerReference, p.resolvedAt = :now "
            + "where p.gatewayReference = :gatewayReference "
            + "and p.status = com.jamlapps.meridian.payment.PaymentStatus.AWAITING_CONFIRMATION")
    int tryConfirm(String gatewayReference, String providerReference, Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PaymentTransactionEntity p set p.status = com.jamlapps.meridian.payment.PaymentStatus.FAILED, "
            + "p.providerTransactionReference = :providerReference, p.resolvedAt = :now "
            + "where p.gatewayReference = :gatewayReference "
            + "and p.status = com.jamlapps.meridian.payment.PaymentStatus.AWAITING_CONFIRMATION")
    int tryFail(String gatewayReference, String providerReference, Instant now);

    /** BR-ADM-03: candidates for {@code PaymentTimeoutSweeper} — initiated long enough ago with no callback yet. */
    List<PaymentTransactionEntity> findByStatusAndInitiatedAtBefore(PaymentStatus status, Instant cutoff);

    /**
     * BR-ADM-03: the timeout transition, atomic for the same reason {@link #tryConfirm} is — a
     * genuinely late callback racing the sweeper can only win one of the two outcomes, never both.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update PaymentTransactionEntity p set p.status = com.jamlapps.meridian.payment.PaymentStatus.TIMED_OUT, p.resolvedAt = :now "
            + "where p.id = :id and p.status = com.jamlapps.meridian.payment.PaymentStatus.AWAITING_CONFIRMATION")
    int tryTimeout(UUID id, Instant now);

    /** BR-ADM-03: the exception queue — unreconciled (still {@code AWAITING_CONFIRMATION}), timed-out, and failed. */
    List<PaymentTransactionEntity> findByStatusInOrderByInitiatedAtDesc(List<PaymentStatus> statuses);
}
