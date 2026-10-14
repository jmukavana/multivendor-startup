package com.jamlapps.meridian.inventory.internal;

import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** AC-INV-04: every movement, with its reason and a reference to the causing transaction. */
record StockMovementView(int quantityDelta, StockMovementEntity.Reason reason, @Nullable UUID referenceId, Instant occurredAt) {}
