package com.jamlapps.meridian.inventory.internal;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface StockRepository extends JpaRepository<StockEntity, UUID> {

    Optional<StockEntity> findByProductId(UUID productId);

    /**
     * BR-INV-04, AC-INV-01: the concurrency-safety mechanism. This is a single atomic UPDATE with
     * the availability check in its own WHERE clause — {@code total_quantity - reserved_quantity
     * >= :quantity} — not a read-then-write. Two simultaneous reservations against one unit of
     * stock race at the database row lock, and PostgreSQL guarantees only one UPDATE can match and
     * apply; the other evaluates the WHERE clause against the now-updated row and affects zero
     * rows. There is no window between reading availability and writing a reservation for a second
     * request to land in — reading it in Java first and checking it in Java second is exactly the
     * race this avoids.
     *
     * @return 1 if the reservation was accepted, 0 if there was insufficient stock
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockEntity s set s.reservedQuantity = s.reservedQuantity + :quantity, s.updatedAt = :now "
            + "where s.productId = :productId and (s.totalQuantity - s.reservedQuantity) >= :quantity")
    int tryReserve(UUID productId, int quantity, Instant now);

    /** BR-INV-03/06: gives back what a reservation held. Same atomicity reasoning as {@link #tryReserve}. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockEntity s set s.reservedQuantity = s.reservedQuantity - :quantity, s.updatedAt = :now "
            + "where s.productId = :productId")
    int release(UUID productId, int quantity, Instant now);

    /**
     * BR-INV-05, AC-INV-03: the only operation that reduces {@code totalQuantity}. Both counters
     * move together in one statement, so "total stock reduced exactly once" is a property of this
     * query running exactly once per reservation — enforced by {@code StockReservationEntity}'s
     * status transition, not by anything here.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockEntity s set s.totalQuantity = s.totalQuantity - :quantity, "
            + "s.reservedQuantity = s.reservedQuantity - :quantity, s.updatedAt = :now where s.productId = :productId")
    int confirm(UUID productId, int quantity, Instant now);

    /** BR-INV-01: a direct restock, atomic for the same reason as {@link #tryReserve}. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockEntity s set s.totalQuantity = :newTotal, s.updatedAt = :now where s.productId = :productId")
    int setTotalQuantity(UUID productId, int newTotal, Instant now);

    /**
     * BR-ORD-14: the exact inverse of {@link #confirm} — gives back {@code quantity} to {@code
     * totalQuantity} alone, leaving {@code reservedQuantity} untouched. {@link #confirm} reduced
     * both counters together when the reservation was consumed; this reservation is not being
     * re-reserved, only made available again, so only the counter {@code confirm} touched moves.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update StockEntity s set s.totalQuantity = s.totalQuantity + :quantity, s.updatedAt = :now where s.productId = :productId")
    int restock(UUID productId, int quantity, Instant now);
}
