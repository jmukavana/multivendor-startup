package com.jamlapps.meridian.order;

import java.util.List;
import java.util.UUID;

/**
 * BR-SEC-04: the buyer-side half of "return rate, dispute rate and cancellation rate" —
 * cancellation, the one BR-SEC-04's own signal that {@code order} owns outright. Deliberately an
 * aggregate query, not a per-buyer lookup like every other cross-module read in this codebase
 * ({@code Accounts}, {@code Vendors}, {@code Products}): BR-SEC-04 asks for a platform median,
 * which needs every buyer's rate at once to compute, not one buyer's rate resolved on demand.
 */
public interface BuyerOrderActivity {

    List<BuyerActivityStat> cancellationStats();

    record BuyerActivityStat(UUID buyerAccountId, long totalSubOrders, long cancelledSubOrders) {}
}
