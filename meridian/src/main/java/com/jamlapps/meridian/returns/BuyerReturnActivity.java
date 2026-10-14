package com.jamlapps.meridian.returns;

import java.util.List;
import java.util.UUID;

/**
 * BR-SEC-04: the "return rate" and "dispute rate" halves of the buyer-abuse signal. {@code
 * disputedReturns} counts a return as disputed once it has reached {@link ReturnStatus#ESCALATED}
 * or been resolved {@link ReturnStatus#RESOLVED_VENDOR} — {@code RESOLVED_VENDOR} is only ever
 * reached through an admin resolving an escalation, so it necessarily went through dispute.
 * {@code RESOLVED_BUYER} does not: it is reached either by a vendor directly accepting (never
 * disputed) or by an admin resolving an escalation in the buyer's favour (was disputed) — this
 * codebase's current data model does not distinguish the two, so a {@code RESOLVED_BUYER} return
 * is never counted as disputed here. This under-counts real disputes rather than over-counting
 * them — a deliberate choice, not an oversight; see {@code integrity.package-info} for the fuller
 * reasoning.
 */
public interface BuyerReturnActivity {

    List<BuyerReturnStat> returnStats();

    record BuyerReturnStat(UUID buyerAccountId, long totalReturns, long disputedReturns) {}
}
