package com.jamlapps.meridian.inventory.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.jamlapps.meridian.TestcontainersConfiguration;
import com.jamlapps.meridian.inventory.InsufficientStockException;
import com.jamlapps.meridian.inventory.ReservationOutcome;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AC-INV-01 to 04, exercised directly against {@code inventory}'s own services rather than through
 * HTTP — this is the module's internal concurrency and expiry mechanics, not a caller-facing flow,
 * and living in {@code inventory.internal} gives the test package-private access to
 * {@link InventoryService}, {@link StockManagementService} and {@link ReservationExpirySweeper}
 * directly rather than through their (not yet built, as of this test) HTTP surface.
 *
 * <p>Account, vendor and product rows are seeded directly via JDBC rather than through the real
 * registration and vendor-approval HTTP flow ({@code WalkingSkeletonFlowTests} already covers that
 * flow end to end) — this test's subject is stock arithmetic under contention, which needs a
 * product to exist and does not need to prove how one gets approved.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class InventoryConcurrencyTests {

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    StockReservationRepository reservationRepository;

    @Autowired
    StockMovementRepository movementRepository;

    @Autowired
    InventoryService inventoryService;

    @Autowired
    StockManagementService stockManagementService;

    @Autowired
    ReservationExpirySweeper sweeper;

    UUID accountId;
    UUID vendorId;
    UUID productId;

    @BeforeEach
    void seedVendorAndProduct() {
        accountId = UUID.randomUUID();
        vendorId = UUID.randomUUID();
        productId = UUID.randomUUID();
        Instant now = Instant.now();

        jdbc.update(
                "insert into account (id, phone_number, display_name, status, created_at, updated_at) "
                        + "values (?, ?, 'Test Vendor', 'ACTIVE', ?, ?)",
                accountId,
                "+254700" + (100000 + Math.abs(accountId.hashCode() % 900000)),
                Timestamp.from(now),
                Timestamp.from(now));

        jdbc.update(
                "insert into vendor (id, owner_account_id, business_name, status, created_at) "
                        + "values (?, ?, 'Test Vendor Business', 'ACTIVE', ?)",
                vendorId,
                accountId,
                Timestamp.from(now));

        jdbc.update(
                "insert into product (id, vendor_id, name, price_minor_units, currency, status, created_at, updated_at) "
                        + "values (?, ?, 'Test Product', 1000, 'KES', 'ACTIVE', ?, ?)",
                productId,
                vendorId,
                Timestamp.from(now),
                Timestamp.from(now));
    }

    @Test
    @DisplayName("AC-INV-01: two simultaneous reservations against one unit — exactly one succeeds")
    void concurrentReservationsAgainstOneUnit() throws Exception {
        stockManagementService.setTotalQuantity(productId, accountId, 1);

        int attempts = 8;
        ExecutorService pool = Executors.newFixedThreadPool(attempts);
        CountDownLatch ready = new CountDownLatch(attempts);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger refused = new AtomicInteger();

        List<Future<?>> futures = new java.util.ArrayList<>();
        for (int i = 0; i < attempts; i++) {
            futures.add(pool.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    inventoryService.reserve(productId, 1, UUID.randomUUID());
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException expected) {
                    refused.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        ready.await(5, TimeUnit.SECONDS);
        // All eight threads are parked on the latch before any of them calls reserve(), so the
        // race is genuinely simultaneous rather than staggered by thread start-up time.
        go.countDown();
        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        pool.shutdown();

        assertThat(succeeded.get()).as("exactly one of %d simultaneous attempts should succeed", attempts).isEqualTo(1);
        assertThat(refused.get()).isEqualTo(attempts - 1);

        var stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.available()).isZero();
        assertThat(stock.reservedQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("AC-INV-02: an expired reservation releases stock back to available")
    void expiredReservationReleasesStock() {
        stockManagementService.setTotalQuantity(productId, accountId, 5);

        var reservation = inventoryService.reserve(productId, 2, UUID.randomUUID());
        assertThat(stockManagementService.availableQuantity(productId)).isEqualTo(3);

        // Simulate the 15-minute TTL having already elapsed, rather than waiting for it in the
        // test — the mechanism under test is the sweep, not the clock.
        jdbc.update(
                "update stock_reservation set expires_at = ? where id = ?",
                Timestamp.from(Instant.now().minusSeconds(60)),
                reservation.id());

        sweeper.sweep();

        assertThat(stockManagementService.availableQuantity(productId)).isEqualTo(5);
        var resolved = reservationRepository.findById(reservation.id()).orElseThrow();
        assertThat(reservationField(resolved)).isEqualTo(ReservationOutcome.RELEASED);

        boolean hasExpiredMovement = movementRepository.findByProductIdOrderByOccurredAtDesc(productId).stream()
                .anyMatch(m -> m.reason() == StockMovementEntity.Reason.EXPIRED);
        assertThat(hasExpiredMovement).as("an EXPIRED movement should be recorded").isTrue();
    }

    @Test
    @DisplayName("AC-INV-03: confirming a reservation reduces total stock exactly once")
    void confirmReducesTotalExactlyOnce() {
        stockManagementService.setTotalQuantity(productId, accountId, 10);
        var reservation = inventoryService.reserve(productId, 3, UUID.randomUUID());

        UUID parentOrderId = reservation.referenceId();
        inventoryService.confirmForOrder(parentOrderId);

        var stock = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stock.totalQuantity()).isEqualTo(7);
        assertThat(stock.reservedQuantity()).isZero();

        // Calling confirm again for the same order must not deduct a second time — the query only
        // matches reservations still in RESERVED status, and this one is now CONFIRMED.
        inventoryService.confirmForOrder(parentOrderId);
        var stockAfterSecondCall = stockRepository.findByProductId(productId).orElseThrow();
        assertThat(stockAfterSecondCall.totalQuantity()).isEqualTo(7);
    }

    private ReservationOutcome reservationField(StockReservationEntity entity) {
        return entity.toReservation().status();
    }
}
