/**
 * Platform integrity and abuse controls.
 *
 * <p>Implements BR-SEC of the business requirements document. Delivered in phase 9, with BR-SEC-01
 * and BR-SEC-02 built for the endpoints they protect regardless of which module those endpoints
 * live in — the BRD groups these requirements by concern, not by module, and enforcing a rate
 * limit only where {@code integrity}'s own controllers happen to sit would satisfy the package
 * boundary while leaving the actual endpoints unprotected.
 *
 * <p><strong>{@code web.RateLimiter} is the real mechanism, and it is not owned here.</strong>
 * {@code iam}, {@code checkout} and {@code search} each call it directly, at exactly the endpoint
 * BR-SEC-01 names for them — OTC issuance and verification, checkout initiation, search — without
 * any of the three gaining a dependency on this module. That is only possible because {@code web}
 * is the OPEN, foundational module every other module can already use: the interface lives there,
 * as a slot, and {@code integrity.internal.RateLimiterService} is the one bean that fills it. See
 * {@code web.RateLimiter}'s own javadoc for the full reasoning and the precedent this follows.
 * {@code integrity}'s own {@code allowedDependencies} — just {@code iam}, matching ADR-002's table
 * exactly — has nothing to do with which endpoints get rate-limited; nothing in this module ever
 * calls another module's code to enforce BR-SEC-01, and {@code iam} needing {@code RateLimiter} is
 * the reverse direction from a dependency, resolved entirely inside {@code web}.
 *
 * <p><strong>BR-SEC-01's "password resets" has nothing to protect.</strong> This platform is
 * OTP-only (BR-IAM-01/02) — there is no password anywhere in {@code iam}, and so no password-reset
 * endpoint for a rate limit to guard. Recorded as a requirement with no applicable target in this
 * codebase, not a gap in this module's coverage of it.
 *
 * <p><strong>BR-SEC-06 (overselling detection) is built in {@code inventory}, not here.</strong>
 * {@code inventory.internal.OversellAttemptEntity} records the signal at the exact point BR-INV-04's
 * database constraint already prevents true overselling from ever landing — the reservation
 * attempt that would have exceeded available stock. Putting the record where the constraint lives
 * keeps the detection and the invariant it is detecting a near-miss against in the same place;
 * routing it through {@code integrity} would need a dependency neither module's boundary grants
 * and buys nothing a shared table wouldn't. See that class's javadoc.
 *
 * <p><strong>BR-SEC-03 lives in {@code order} and {@code checkout}, not here.</strong>
 * {@code order.ParentOrder} now carries {@code sourceAddress}/{@code userAgent}, captured once at
 * checkout (the same request the BR-SEC-01 rate limit above already reads {@code sourceAddress}
 * from) and surfaced through {@code order.Orders.findBySourceAddress} — folded into {@code
 * admin.internal.AdminOrderSearchController}'s existing BR-ADM-02 search as a fifth criterion,
 * since "who else ordered from this address" is exactly the cross-vendor search that endpoint
 * already provides. Nothing about that needed {@code integrity} at all.
 *
 * <p><strong>BR-SEC-04/05's "materially above the platform median" is real, in {@code
 * AbuseDetectionService}.</strong> This module's {@code allowedDependencies} grew to include
 * {@code order}, {@code returns}, {@code fulfilment} and {@code vendor} — a real superset of
 * ADR-002's table, taken deliberately: BR-SEC-04/05 fundamentally need every buyer's or vendor's
 * rate at once to compute a median against, which cannot be resolved from an event payload the
 * way {@code notification}'s dispatch/delivery gap turned out to be (see that module's package
 * doc) — a median is inherently a cross-account read, not a per-account lookup. Each owning module
 * exposes only its own raw counts through a narrow, read-only interface — {@code
 * order.BuyerOrderActivity} (cancellations), {@code returns.BuyerReturnActivity} (returns,
 * disputes), {@code fulfilment.VendorDispatchActivity} (dispatch failures), {@code
 * vendor.Vendors.vendorsInPayoutCoolingOff} (reusing BR-VND-09's existing cooling-off state, not a
 * new mechanism) — and this module does the aggregation, median computation and "materially above"
 * flagging alone; none of those counts are recomputed elsewhere.
 *
 * <p><strong>BR-SEC-05's "stock-out rate on accepted orders" is not built.</strong> Unlike dispatch
 * failure (a real {@code DeliveryStatus.FAILED} count already exists) or payout timing (BR-VND-09's
 * cooling-off already exists), nothing in this codebase tracks "a vendor accepted a sub-order, then
 * could not fulfil it for lack of stock" as a distinct, queryable event — {@code
 * OrderService.accept} has no failure path of that shape, and inventing one to serve this one Should
 * -priority sub-metric is out of scope for this pass. The other two BR-SEC-05 signals are real.
 *
 * <p><strong>A real bug, not a design choice from the start:</strong> the first version of {@code
 * RateLimiterService} recorded a breach's {@code integrity_event} row and any resulting {@code
 * Restriction} in the same transaction as the {@code tryConsume} call that found the limit
 * exceeded. Every caller that reaches a breach does so on its way to throwing {@code
 * ApiException(RATE_LIMITED)}, which rolls that whole transaction back — erasing the very audit
 * row and restriction the breach was supposed to leave behind, every single time, since a breach
 * and a thrown exception are the same event. A test exercising three real breaches in a row and
 * expecting an escalated, admin-visible restriction (BR-SEC-07/08) failed with an empty
 * restriction list, which is what surfaced this. Fixed by moving the breach bookkeeping into
 * {@code RateLimitBreachRecorder}, a separate bean whose {@code REQUIRES_NEW} transaction commits
 * independently of the caller's rollback — the same fix, for the same reason, as {@code
 * inventory.internal.OversellAttemptRecorder}. The bucket increment itself needed no such fix; see
 * {@code RateLimiterService}'s own javadoc for why.
 *
 * <p>BR-SEC-07's constraint governs the one piece of state this module does own: {@link
 * com.jamlapps.meridian.integrity.Restriction} is created only as an audit trail alongside a rate
 * limit breach ({@code RateLimiterService.recordBreach}), never as a separate enforcement path, and
 * is reversed only in full, only by an admin ({@code RestrictionService.lift}) — there is no code
 * path anywhere in this module that disables or terminates an account.
 *
 * <p><strong>{@code allowedDependencies} still has no {@code iam}</strong> — ADR-002 anticipated one
 * for account-level pattern detection, but nothing built here, including BR-SEC-04/05's analytics,
 * ever needs to resolve an id back to an {@code iam.Account}: {@code AbuseDetectionService} works
 * entirely in ids and counts, and {@code AdminAbuseController} returns them as-is for an admin to
 * cross-reference elsewhere. {@code order}, {@code returns}, {@code fulfilment} and {@code vendor}
 * are real superset additions instead, for the reasons above.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Platform Integrity",
        allowedDependencies = {"order", "returns", "fulfilment", "vendor", "web"})
package com.jamlapps.meridian.integrity;
