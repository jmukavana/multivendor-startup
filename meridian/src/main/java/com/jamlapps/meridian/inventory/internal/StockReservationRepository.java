package com.jamlapps.meridian.inventory.internal;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockReservationRepository extends JpaRepository<StockReservationEntity, UUID> {

    Optional<StockReservationEntity> findByIdAndStatus(UUID id, com.jamlapps.meridian.inventory.ReservationOutcome status);

    List<StockReservationEntity> findByReferenceId(UUID referenceId);

    List<StockReservationEntity> findByReferenceIdAndStatus(
            UUID referenceId, com.jamlapps.meridian.inventory.ReservationOutcome status);

    /** BR-ORD-14: resolves which reservation a cancelled sub-order's line refers to, out of possibly several vendors' reservations sharing one parent order's {@code referenceId}. */
    Optional<StockReservationEntity> findByReferenceIdAndProductId(UUID referenceId, UUID productId);

    /** BR-INV-03: the expiry sweep's query. */
    List<StockReservationEntity> findByStatusAndExpiresAtBefore(
            com.jamlapps.meridian.inventory.ReservationOutcome status, Instant cutoff);
}
