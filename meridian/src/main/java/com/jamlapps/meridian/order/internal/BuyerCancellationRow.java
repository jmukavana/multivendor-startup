package com.jamlapps.meridian.order.internal;

import java.util.UUID;

/** BR-SEC-04: one buyer's aggregate cancellation counts — {@code SubOrderRepository.cancellationStatsByBuyer}'s JPQL constructor-expression target. */
record BuyerCancellationRow(UUID buyerAccountId, long total, long cancelled) {}
