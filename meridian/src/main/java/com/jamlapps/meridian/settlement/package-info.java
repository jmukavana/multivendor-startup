/**
 * Settlement.
 *
 * <p>Implements BR-STL of the business requirements document. Delivered in phase 6.
 * Allowed dependencies: ledger, vendor.
 *
 * <p>ADR-002's table lists {@code order}, {@code returns} and {@code pricing} here too. {@code
 * returns} is absent because that module (Phase 5) does not exist yet — BR-STL-04/05, which need
 * it, are not implemented in this pass; see {@code V14__settlement.sql}'s header comment. {@code
 * order} and {@code pricing} are absent because nothing in this pass needs them yet: BR-STL-01's
 * entitlement is read straight off {@code ledger}'s already-posted VENDOR_PAYABLE balance, which
 * was computed against catalog and commission state at posting time — settlement does not need to
 * re-derive it from either module. BR-STL-03's per-order itemised statement (rather than the
 * current per-vendor total) is the reason both are in ADR-002's table at all, and is the reason to
 * add them back, deliberately, when that itemisation is actually built.
 *
 * <p><strong>BR-ADM-06's settlement cycle is real, closing a deferred gap, and lives entirely
 * here — not in {@code pricing} alongside commission rate and holdback.</strong> Commission and
 * holdback are per-order facts COM-05 requires be reproducible for any historical order, which is
 * why {@code order} resolves and stores a version id for each at creation. A settlement cycle is a
 * different kind of thing: not a fact about an order at all, but a scheduling policy — "how often
 * may this vendor be settled" — evaluated fresh every time {@code SettlementService.initiate}
 * calculates a run, the same way BR-STL-08's cooling-off check already is. Routing it through
 * {@code pricing} would buy nothing and would misrepresent it as a per-order fact it is not; see
 * {@code SettlementCycle}'s javadoc. {@code SettlementCycleConfigurationEntity} is still versioned
 * and never updated in place (DAT-07), and {@code SettlementService.createCycleConfiguration}
 * writes to BR-ADM-07's audit log via {@code web.AuditLog} — the same OPEN-module slot {@code
 * pricing.internal.PricingService} already uses, needing no new dependency here either.
 * {@code SettlementController} exposes it directly ({@code POST /settlement/config/cycle}), the
 * same "admin lives in the owning module" precedent {@code fulfilment.internal
 * .AdminFulfilmentController} already set for delivery zones and rate cards.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Settlement",
        allowedDependencies = { "ledger", "vendor" })
package com.jamlapps.meridian.settlement;
