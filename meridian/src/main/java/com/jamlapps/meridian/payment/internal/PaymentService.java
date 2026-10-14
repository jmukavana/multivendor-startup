package com.jamlapps.meridian.payment.internal;

import com.jamlapps.meridian.payment.PaymentStatus;
import com.jamlapps.meridian.payment.PaymentTransaction;
import com.jamlapps.meridian.payment.Refund;
import com.jamlapps.meridian.payment.RefundStatus;
import com.jamlapps.meridian.payment.events.PaymentConfirmed;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-PAY-01/02/04/05/07/10/11/12. See this module's V12 migration comment for what is deferred. */
@Service
class PaymentService {

    private final PaymentTransactionRepository transactions;
    private final RefundRepository refunds;
    private final PaymentGateway gateway;
    private final RefundGateway refundGateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    PaymentService(
            PaymentTransactionRepository transactions,
            RefundRepository refunds,
            PaymentGateway gateway,
            RefundGateway refundGateway,
            ApplicationEventPublisher events,
            Clock clock) {
        this.transactions = transactions;
        this.refunds = refunds;
        this.gateway = gateway;
        this.refundGateway = refundGateway;
        this.events = events;
        this.clock = clock;
    }

    /**
     * BR-PAY-02. Takes {@code amount} and {@code parentOrderId} from the caller rather than
     * looking an order up — see {@code PaymentInitiation}'s javadoc for why.
     */
    @Transactional
    PaymentTransaction initiate(UUID parentOrderId, Money amount, UUID buyerAccountId, String buyerPhoneNumber) {
        Instant now = clock.instant();

        PaymentTransactionEntity transaction = transactions.save(new PaymentTransactionEntity(
                UUID.randomUUID(), parentOrderId, amount, buyerAccountId, buyerPhoneNumber, now));

        var initiation = gateway.initiateCollection(amount, buyerPhoneNumber, parentOrderId.toString());
        transaction.markAwaitingConfirmation(initiation.gatewayReference());
        transactions.save(transaction);

        return transaction.toPaymentTransaction();
    }

    /**
     * BR-PAY-04/05: the callback endpoint's handler. Idempotent via {@code tryConfirm}/{@code
     * tryFail}'s atomic WHERE clause — see that repository's javadoc — so a replayed callback (the
     * provider's own retry behaviour, not necessarily an attack) is a no-op the second time, not a
     * double-publish of {@link PaymentConfirmed}.
     */
    @Transactional
    PaymentTransaction handleCallback(String gatewayReference, String providerTransactionReference, boolean success) {
        transactions
                .findByGatewayReference(gatewayReference)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.NOT_FOUND, "No payment transaction for gateway reference " + gatewayReference));

        Instant now = clock.instant();

        int updated = success
                ? transactions.tryConfirm(gatewayReference, providerTransactionReference, now)
                : transactions.tryFail(gatewayReference, providerTransactionReference, now);

        PaymentTransactionEntity resolved =
                transactions.findByGatewayReference(gatewayReference).orElseThrow();

        // updated == 0 means this callback was already handled by an earlier delivery of the same
        // event; resolved already reflects that outcome, so nothing more happens here either way.
        if (updated == 1 && success) {
            events.publishEvent(new PaymentConfirmed(
                    resolved.id(), resolved.parentOrderId(), resolved.amount(), providerTransactionReference, now));
        }

        return resolved.toPaymentTransaction();
    }

    /**
     * BR-PAY-11: resolves {@code parentOrderId} to its payment transaction, then instructs a
     * refund through the provider. Deliberately does not touch {@code PaymentTransactionEntity
     * .status} — it can be CONFIRMED with several refunds recorded against it over time (BR-RTN-04's
     * partial refunds), and REVERSED is reserved for a transaction refunded in full, which nothing
     * here tracks yet; see {@code RefundStatus}'s own note on that gap.
     */
    @Transactional
    Refund initiateRefund(UUID parentOrderId, Money amount, String reason) {
        PaymentTransactionEntity transaction = transactions
                .findByParentOrderId(parentOrderId)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.VALIDATION_ERROR, "No payment transaction found for order " + parentOrderId + "."));

        if (transaction.status() != PaymentStatus.CONFIRMED) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "Order " + parentOrderId + "'s payment was never confirmed; nothing to refund.");
        }

        Instant now = clock.instant();
        var result = refundGateway.refund(amount, transaction.gatewayReference(), reason);

        RefundEntity refund = refunds.save(new RefundEntity(
                UUID.randomUUID(),
                transaction.id(),
                amount,
                reason,
                RefundStatus.CONFIRMED,
                result.providerRefundReference(),
                now,
                now));

        return refund.toRefund();
    }

    @Transactional(readOnly = true)
    PaymentTransaction get(UUID paymentTransactionId, UUID callerAccountId) {
        PaymentTransactionEntity transaction = transactions
                .findById(paymentTransactionId)
                .orElseThrow(() -> ApiException.notFound("Payment transaction", paymentTransactionId));

        if (!transaction.buyerAccountId().equals(callerAccountId)) {
            throw ApiException.notFound("Payment transaction", paymentTransactionId);
        }

        return transaction.toPaymentTransaction();
    }

    /**
     * BR-ADM-03's "timed-out" — not yet sourced from configuration, a constant here rather than a
     * business decision made silently, same treatment as {@code inventory.internal.InventoryService
     * .RESERVATION_TTL}. {@link PaymentStatus#TIMED_OUT} exists in the enum from the start but
     * nothing set it until this: a transaction stuck {@code AWAITING_CONFIRMATION} past this window
     * genuinely means the mock gateway's callback path (or a real provider's, later) never arrived.
     */
    static final Duration TIMEOUT_AFTER = Duration.ofMinutes(30);

    @Transactional(readOnly = true)
    List<UUID> timeoutCandidateIds() {
        return transactions.findByStatusAndInitiatedAtBefore(PaymentStatus.AWAITING_CONFIRMATION, clock.instant().minus(TIMEOUT_AFTER)).stream()
                .map(PaymentTransactionEntity::id)
                .toList();
    }

    /** Called from {@code PaymentTimeoutSweeper} per id, through the injected proxy — never looped from inside this class. */
    @Transactional
    void tryTimeout(UUID paymentTransactionId) {
        transactions.tryTimeout(paymentTransactionId, clock.instant());
    }

    /** BR-ADM-03: the admin exception queue — unreconciled, timed-out, and failed transactions. */
    @Transactional(readOnly = true)
    List<PaymentTransaction> exceptionTransactions() {
        return transactions
                .findByStatusInOrderByInitiatedAtDesc(List.of(PaymentStatus.AWAITING_CONFIRMATION, PaymentStatus.TIMED_OUT, PaymentStatus.FAILED))
                .stream()
                .map(PaymentTransactionEntity::toPaymentTransaction)
                .toList();
    }
}
