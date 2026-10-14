package com.jamlapps.meridian.inventory.internal;

import com.jamlapps.meridian.inventory.InsufficientStockException;
import com.jamlapps.meridian.inventory.Reservation;
import com.jamlapps.meridian.inventory.ReservationOutcome;
import com.jamlapps.meridian.inventory.Reservations;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-INV-02 to 06, AC-INV-01 to 04. See {@link StockRepository}'s javadoc for the concurrency
 * mechanism {@link #reserve} depends on, and {@code StockMovementEntity}'s note on what the
 * ledger's signed quantity means for each reason.
 */
@Service
class InventoryService implements Reservations {

    /**
     * BR-INV-02's "configurable expiry period", matching AC-INV-02's own premise exactly. Not yet
     * sourced from configuration — a constant here rather than a business decision made silently,
     * same treatment as {@code PayoutDetailEntity.COOLING_OFF}.
     */
    static final Duration RESERVATION_TTL = Duration.ofMinutes(15);

    private final StockRepository stock;
    private final StockReservationRepository reservations;
    private final StockMovementRepository movements;
    private final OversellAttemptRecorder oversellAttempts;
    private final Clock clock;

    InventoryService(
            StockRepository stock,
            StockReservationRepository reservations,
            StockMovementRepository movements,
            OversellAttemptRecorder oversellAttempts,
            Clock clock) {
        this.stock = stock;
        this.reservations = reservations;
        this.movements = movements;
        this.oversellAttempts = oversellAttempts;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Reservation reserve(UUID productId, int quantity, UUID referenceId) {
        Instant now = clock.instant();

        if (stock.tryReserve(productId, quantity, now) == 0) {
            int available = stock.findByProductId(productId).map(StockEntity::available).orElse(0);
            // BR-SEC-06, RSK-11: recorded at the exact point of deduction the requirement names,
            // in its own transaction (see OversellAttemptRecorder) so it survives the rollback the
            // exception below triggers in the caller.
            oversellAttempts.record(productId, quantity, available, referenceId, now);
            throw new InsufficientStockException(productId, quantity, available);
        }

        StockReservationEntity reservation = reservations.save(new StockReservationEntity(
                UUID.randomUUID(), productId, quantity, referenceId, now.plus(RESERVATION_TTL), now));

        recordMovement(productId, -quantity, StockMovementEntity.Reason.RESERVED, referenceId, now);

        return reservation.toReservation();
    }

    /** BR-INV-06. Idempotent: releasing an already-resolved reservation is a no-op, not an error. */
    @Override
    @Transactional
    public void release(UUID reservationId) {
        reservations
                .findByIdAndStatus(reservationId, ReservationOutcome.RESERVED)
                .ifPresent(reservation -> releaseInternal(reservation, StockMovementEntity.Reason.RELEASED));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reservation> findByReferenceId(UUID referenceId) {
        return reservations.findByReferenceId(referenceId).stream().map(StockReservationEntity::toReservation).toList();
    }

    /**
     * BR-INV-05, AC-INV-03: converts every live reservation for an order into a permanent
     * deduction. Called by {@code OrderPaymentConfirmationListener} on {@code
     * order.events.OrderPaymentConfirmed} — internal to this module, not part of the public
     * {@link Reservations} API, because nothing outside {@code inventory} should be able to
     * trigger a stock deduction directly; only a confirmed payment may.
     */
    @Transactional
    void confirmForOrder(UUID parentOrderId) {
        Instant now = clock.instant();

        for (StockReservationEntity reservation :
                reservations.findByReferenceIdAndStatus(parentOrderId, ReservationOutcome.RESERVED)) {
            stock.confirm(reservation.productId(), reservation.quantity(), now);
            reservation.resolve(ReservationOutcome.CONFIRMED, now);
            reservations.save(reservation);

            // CONFIRMED records the reduction in physical stock on hand, not a change in what is
            // available to buyers — that was already removed from availability when reserved, and
            // confirming does not change it further. See StockMovementEntity's Reason javadoc.
            recordMovement(
                    reservation.productId(), -reservation.quantity(), StockMovementEntity.Reason.CONFIRMED,
                    parentOrderId, now);
        }
    }

    /** BR-INV-03, AC-INV-02: the expiry sweep's action per reservation. Called by {@code ReservationExpirySweeper}. */
    @Transactional
    void expire(UUID reservationId) {
        reservations
                .findByIdAndStatus(reservationId, ReservationOutcome.RESERVED)
                .ifPresent(reservation -> releaseInternal(reservation, StockMovementEntity.Reason.EXPIRED));
    }

    /**
     * BR-ORD-13/14: reacts to a buyer cancellation, one line at a time. Which action applies is
     * the reservation's own current state, not anything the caller ({@code
     * SubOrderCancellationListener}) needs to know or pass in: RESERVED (payment never confirmed)
     * is released exactly like an expiry; CONFIRMED (stock already deducted) is given back via
     * {@link StockRepository#restock}, the one operation this module has that undoes {@link
     * StockRepository#confirm}. A reservation already RELEASED, EXPIRED or CANCELLED is left alone
     * — this method is safe to run against a line more than once.
     */
    @Transactional
    void cancelLine(UUID parentOrderId, UUID productId, int quantity) {
        reservations.findByReferenceIdAndProductId(parentOrderId, productId).ifPresent(reservation -> {
            Instant now = clock.instant();

            if (reservation.status() == ReservationOutcome.RESERVED) {
                releaseInternal(reservation, StockMovementEntity.Reason.RELEASED);
            } else if (reservation.status() == ReservationOutcome.CONFIRMED) {
                stock.restock(productId, quantity, now);
                reservation.resolve(ReservationOutcome.CANCELLED, now);
                reservations.save(reservation);
                recordMovement(productId, quantity, StockMovementEntity.Reason.CANCELLED, parentOrderId, now);
            }
        });
    }

    private void releaseInternal(StockReservationEntity reservation, StockMovementEntity.Reason reason) {
        Instant now = clock.instant();
        stock.release(reservation.productId(), reservation.quantity(), now);
        reservation.resolve(ReservationOutcome.RELEASED, now);
        reservations.save(reservation);
        recordMovement(reservation.productId(), reservation.quantity(), reason, reservation.referenceId(), now);
    }

    private void recordMovement(
            UUID productId, int quantityDelta, StockMovementEntity.Reason reason, @Nullable UUID referenceId, Instant now) {
        movements.save(new StockMovementEntity(UUID.randomUUID(), productId, quantityDelta, reason, referenceId, now));
    }
}
