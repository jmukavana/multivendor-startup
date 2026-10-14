package com.jamlapps.meridian.returns.internal;

import com.jamlapps.meridian.order.OrderLine;
import com.jamlapps.meridian.order.OrderStatus;
import com.jamlapps.meridian.order.Orders;
import com.jamlapps.meridian.order.SubOrderDetail;
import com.jamlapps.meridian.payment.RefundInitiation;
import com.jamlapps.meridian.returns.Dispute;
import com.jamlapps.meridian.returns.DisputeFavor;
import com.jamlapps.meridian.returns.DisputeStatus;
import com.jamlapps.meridian.returns.Return;
import com.jamlapps.meridian.returns.ReturnReason;
import com.jamlapps.meridian.returns.ReturnStatus;
import com.jamlapps.meridian.returns.events.ReturnRefunded;
import com.jamlapps.meridian.shared.BlobStorage;
import com.jamlapps.meridian.shared.Money;
import com.jamlapps.meridian.vendor.Vendors;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** BR-RTN-01 to 08. BR-RTN-09 (return rate tracking, Should) is not built — see this module's V17 migration comment. */
@Service
class ReturnService {

    private final ReturnRepository returns;
    private final DisputeRepository disputes;
    private final Orders orders;
    private final Vendors vendors;
    private final RefundInitiation refundInitiation;
    private final BlobStorage blobStorage;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    ReturnService(
            ReturnRepository returns,
            DisputeRepository disputes,
            Orders orders,
            Vendors vendors,
            RefundInitiation refundInitiation,
            BlobStorage blobStorage,
            ApplicationEventPublisher events,
            Clock clock) {
        this.returns = returns;
        this.disputes = disputes;
        this.orders = orders;
        this.vendors = vendors;
        this.refundInitiation = refundInitiation;
        this.blobStorage = blobStorage;
        this.events = events;
        this.clock = clock;
    }

    record ReturnLineRequest(UUID productId, int quantity) {}

    /**
     * BR-RTN-01/02/04, AC-RTN-05: the refund amount is computed here, from the sub-order's actual
     * purchased lines and quantities against what the buyer requests back — never taken as a
     * buyer-supplied figure, which would be a straightforward abuse vector (RSK-08).
     */
    @Transactional
    Return request(
            UUID subOrderId,
            UUID buyerAccountId,
            ReturnReason reason,
            @Nullable String description,
            List<ReturnLineRequest> lines) {
        SubOrderDetail subOrder = orders.findSubOrder(subOrderId).orElseThrow(() -> ApiException.notFound("SubOrder", subOrderId));

        // NOT_FOUND, not FORBIDDEN — the same cross-buyer disclosure reasoning as OrderService.get.
        if (!subOrder.buyerAccountId().equals(buyerAccountId)) {
            throw ApiException.notFound("SubOrder", subOrderId);
        }

        if (subOrder.status() != OrderStatus.DELIVERED || subOrder.deliveredAt() == null) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Sub-order " + subOrderId + " has not been delivered.");
        }

        Instant now = clock.instant();
        Instant windowCloses = subOrder.deliveredAt().plus(ReturnEntity.RETURN_WINDOW);
        if (now.isAfter(windowCloses)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR,
                    "The return window for sub-order " + subOrderId + " closed on " + windowCloses + ".");
        }

        if (lines.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "A return needs at least one line.");
        }

        // A second REQUESTED return on the same sub-order is refused outright; a buyer still
        // awaiting a first response has nowhere sensible for a second, overlapping request to go.
        // Multiple *separate, non-overlapping-in-time* returns against different lines of the same
        // sub-order are not guarded against beyond this — a known, documented reduction, not a
        // claim this fully closes RSK-08 on its own.
        if (hasPendingReturn(subOrderId)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_ERROR, "Sub-order " + subOrderId + " already has a return request pending.");
        }

        Money refundAmount = Money.zero(subOrder.subtotal().currency());
        for (ReturnLineRequest requested : lines) {
            OrderLine purchased = subOrder.lines().stream()
                    .filter(line -> line.productId().equals(requested.productId()))
                    .findFirst()
                    .orElseThrow(() -> new ApiException(
                            ErrorCode.VALIDATION_ERROR, "Product " + requested.productId() + " was not part of this sub-order."));

            if (requested.quantity() <= 0 || requested.quantity() > purchased.quantity()) {
                throw ApiException.validation(
                        "invalid quantity",
                        Map.of("quantity", List.of("must be between 1 and " + purchased.quantity())));
            }

            refundAmount = refundAmount.plus(purchased.unitPrice().times(requested.quantity()));
        }

        ReturnEntity entity = returns.save(new ReturnEntity(
                UUID.randomUUID(),
                subOrderId,
                subOrder.parentOrderId(),
                buyerAccountId,
                subOrder.vendorId(),
                reason,
                description,
                refundAmount,
                now));

        return entity.toReturn();
    }

    private boolean hasPendingReturn(UUID subOrderId) {
        return returns.findBySubOrderIdAndStatus(subOrderId, ReturnStatus.REQUESTED).isPresent();
    }

    /** BR-RTN-02: attaches one supporting image. Buyer-only, matching who is allowed to create the return itself. */
    @Transactional
    void addImage(UUID returnId, UUID callerAccountId, InputStream content, String contentType, long contentLength) {
        ReturnEntity entity = returns.findById(returnId).orElseThrow(() -> ApiException.notFound("Return", returnId));

        if (!entity.buyerAccountId().equals(callerAccountId)) {
            throw ApiException.notFound("Return", returnId);
        }

        String reference = blobStorage.store(content, contentType, contentLength);
        entity.addImage(UUID.randomUUID(), reference, contentType, clock.instant());
        returns.save(entity);
    }

    /**
     * BR-RTN-03: accept refunds immediately; reject escalates to a {@link Dispute}. Repo-level
     * vendor filtering via {@code findByIdAndVendorId}, matching {@code
     * order.internal.OrderService.markDelivered} exactly.
     */
    @Transactional
    Return vendorRespond(UUID returnId, UUID callerAccountId, boolean accept) {
        UUID vendorId = vendors
                .findByOwnerAccountId(callerAccountId)
                .orElseThrow(() -> new ApiException(ErrorCode.FORBIDDEN, "Only a vendor may respond to a return."))
                .id();

        ReturnEntity entity = returns.findByIdAndVendorId(returnId, vendorId).orElseThrow(() -> ApiException.notFound("Return", returnId));

        if (entity.status() != ReturnStatus.REQUESTED) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Return " + returnId + " has already been resolved or escalated.");
        }

        Instant now = clock.instant();
        if (accept) {
            issueRefund(entity, now);
        } else {
            entity.escalate();
            returns.save(entity);
            disputes.save(new DisputeEntity(UUID.randomUUID(), entity.id(), now));
        }

        return entity.toReturn();
    }

    /**
     * BR-RTN-11/07, AC-RTN-04: {@code reason} non-blank is validated by the controller, not here —
     * matching {@code AdminVendorApplicationController}'s {@code RejectRequest} precedent.
     */
    @Transactional
    Dispute resolveDispute(UUID disputeId, UUID adminAccountId, DisputeFavor favor, String reason) {
        DisputeEntity dispute = disputes.findById(disputeId).orElseThrow(() -> ApiException.notFound("Dispute", disputeId));

        if (dispute.status() != DisputeStatus.OPEN) {
            throw new ApiException(ErrorCode.VALIDATION_ERROR, "Dispute " + disputeId + " is already resolved.");
        }

        ReturnEntity entity = returns
                .findById(dispute.returnId())
                .orElseThrow(() -> new IllegalStateException("Dispute " + disputeId + " references a missing return."));

        Instant now = clock.instant();
        dispute.resolve(favor, reason, adminAccountId, now);
        disputes.save(dispute);

        if (favor == DisputeFavor.BUYER) {
            issueRefund(entity, now);
        } else {
            entity.resolve(ReturnStatus.RESOLVED_VENDOR, now);
            returns.save(entity);
        }

        return dispute.toDispute();
    }

    /**
     * BR-PAY-11/BR-RTN-05: the two things that must both happen for a "the buyer is refunded"
     * outcome — the actual money movement via {@code payment}, and ledger's commission and
     * holdback reversal, announced by {@link ReturnRefunded} rather than called directly (this
     * module has no {@code ledger} dependency; see {@code ReturnRefunded}'s own javadoc for why
     * the event carries the resolved rates).
     */
    private void issueRefund(ReturnEntity entity, Instant now) {
        SubOrderDetail subOrder = orders
                .findSubOrder(entity.subOrderId())
                .orElseThrow(() -> new IllegalStateException("Sub-order " + entity.subOrderId() + " vanished after its return was created."));

        refundInitiation.initiate(entity.parentOrderId(), entity.refundAmount(), "Return " + entity.id());

        entity.resolve(ReturnStatus.RESOLVED_BUYER, now);
        returns.save(entity);

        events.publishEvent(new ReturnRefunded(
                entity.id(),
                entity.subOrderId(),
                entity.vendorId(),
                entity.parentOrderId(),
                entity.refundAmount(),
                subOrder.commissionRate(),
                subOrder.commissionConfigurationId(),
                subOrder.holdbackRate(),
                subOrder.holdbackConfigurationId(),
                now));
    }

    /** BR-RTN-03: called from {@code ReturnTimeoutSweeper}, one id at a time — never looped from inside this class; see {@code settlement.internal.SettlementService}'s class doc for why that distinction is load-bearing, not stylistic. */
    @Transactional
    void escalateIfTimedOut(UUID returnId) {
        ReturnEntity entity = returns.findById(returnId).orElseThrow();

        if (entity.status() != ReturnStatus.REQUESTED) {
            return;
        }

        entity.escalate();
        returns.save(entity);
        disputes.save(new DisputeEntity(UUID.randomUUID(), entity.id(), clock.instant()));
    }

    @Transactional(readOnly = true)
    List<UUID> timedOutReturnIds() {
        Instant cutoff = clock.instant().minus(ReturnEntity.VENDOR_RESPONSE_WINDOW);
        return returns.findByStatusAndRequestedAtBefore(ReturnStatus.REQUESTED, cutoff).stream()
                .map(ReturnEntity::id)
                .toList();
    }

    /** BR-RTN-06: the admin dispute queue. */
    @Transactional(readOnly = true)
    List<Dispute> disputeQueue() {
        return disputes.findByStatusOrderByCreatedAt(DisputeStatus.OPEN).stream().map(DisputeEntity::toDispute).toList();
    }

    @Transactional(readOnly = true)
    Return get(UUID returnId, UUID callerAccountId) {
        ReturnEntity entity = returns.findById(returnId).orElseThrow(() -> ApiException.notFound("Return", returnId));

        boolean isBuyer = entity.buyerAccountId().equals(callerAccountId);
        boolean isVendorOwner = vendors.findByOwnerAccountId(callerAccountId).map(v -> v.id().equals(entity.vendorId())).orElse(false);

        if (!isBuyer && !isVendorOwner) {
            throw ApiException.notFound("Return", returnId);
        }

        return entity.toReturn();
    }

    @Transactional(readOnly = true)
    List<Return> listMine(UUID buyerAccountId) {
        return returns.findByBuyerAccountIdOrderByRequestedAtDesc(buyerAccountId).stream().map(ReturnEntity::toReturn).toList();
    }
}
