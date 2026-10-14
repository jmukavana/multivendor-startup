package com.jamlapps.meridian.inventory.internal;

import java.time.Instant;
import java.util.UUID;

/** BR-SEC-06: a refused reservation attempt, as the admin exception queue sees it. */
record OversellAttemptView(UUID id, UUID productId, int requestedQuantity, int availableQuantity, UUID referenceId, Instant occurredAt) {}
