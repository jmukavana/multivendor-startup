package com.jamlapps.meridian.inventory;

import java.util.List;
import java.util.UUID;

/**
 * BR-INV-02/03/04/05: the reservation lifecycle other modules drive. {@code checkout} reserves and
 * releases as part of its own orchestration (see that module's {@code CheckoutService}); nothing
 * confirms reservations synchronously — that happens on {@code order.events.OrderPaymentConfirmed},
 * consumed internally, per ADR-004.
 */
public interface Reservations {

    /**
     * @throws InsufficientStockException if {@code quantity} exceeds what is currently available
     */
    Reservation reserve(UUID productId, int quantity, UUID referenceId);

    /** BR-INV-06: cancellation or a failed checkout releases what was held. */
    void release(UUID reservationId);

    List<Reservation> findByReferenceId(UUID referenceId);
}
