package com.jamlapps.meridian.returns.internal;

import java.util.UUID;

/** BR-SEC-04: one buyer's aggregate return counts — {@code ReturnRepository.returnStatsByBuyer}'s JPQL constructor-expression target. */
record BuyerReturnRow(UUID buyerAccountId, long total, long disputed) {}
