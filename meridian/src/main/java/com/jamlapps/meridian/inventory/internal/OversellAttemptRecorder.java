package com.jamlapps.meridian.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-SEC-06: a separate bean, not a method on {@link InventoryService}, specifically so {@link
 * #record}'s {@code REQUIRES_NEW} runs in its own transaction rather than {@code reserve}'s.
 * {@code reserve} throws {@link com.jamlapps.meridian.inventory.InsufficientStockException} right
 * after this call, and — as {@code WalkingSkeletonFlowTests.checkoutRefusesInsufficientStockAndCreatesNoOrder}
 * already established — that exception rolls back everything in the caller's transaction, which
 * for a checkout is the whole order. The oversell signal must survive that rollback to be worth
 * anything; committing it in the same transaction it is reporting a failure inside of would erase
 * it the instant that transaction rolls back. Self-invocation from inside {@code InventoryService}
 * would not get this: {@code REQUIRES_NEW} only applies through the Spring proxy, which a call to
 * {@code this.record(...)} bypasses — the same caveat this codebase has hit before.
 */
@Component
class OversellAttemptRecorder {

    private final OversellAttemptRepository oversellAttempts;

    OversellAttemptRecorder(OversellAttemptRepository oversellAttempts) {
        this.oversellAttempts = oversellAttempts;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(UUID productId, int requestedQuantity, int availableQuantity, UUID referenceId, Instant occurredAt) {
        oversellAttempts.save(new OversellAttemptEntity(
                UUID.randomUUID(), productId, requestedQuantity, availableQuantity, referenceId, occurredAt));
    }
}
