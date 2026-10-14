/**
 * Administration and back-office.
 *
 * <p>Implements BR-ADM of the business requirements document. Delivered from Phase 2 onward,
 * completed in Phase 9 — the last module this codebase builds.
 *
 * <p><strong>{@code allowedDependencies} is {@code order}, {@code payment} and {@code pricing} —
 * a real subset of ADR-002's fuller {@code vendor, catalog, order, payment, ledger, settlement,
 * returns, pricing, integrity}.</strong> Every module this session has built already put its own
 * admin surface in its own {@code internal} package the moment BR-ADM needed one, ahead of this
 * module existing — {@code vendor.internal.AdminVendorApplicationController} (BR-ADM-01),
 * {@code returns.internal.AdminDisputeController} (BR-ADM-04), {@code fulfilment.internal
 * .AdminFulfilmentController} (part of BR-ADM-06), and, this same phase,
 * {@code catalog.internal.AdminCatalogController} (BR-ADM-05) and {@code payment.internal
 * .AdminPaymentController} (BR-ADM-03) — every one of them method-secured, none of them waiting
 * for this module. Duplicating any of those here would be two paths to the same state, one of
 * them stale the moment the other changes. This module depends only on what it actually calls:
 * {@code order} and {@code payment} for BR-ADM-02's cross-vendor order search, {@code pricing} for
 * BR-ADM-06's commission-rate piece. {@code vendor}, {@code catalog}, {@code ledger}, {@code
 * settlement}, {@code returns} and {@code integrity} would only earn a place here if this module
 * grew a true aggregated dashboard reading across all of them at once — a real feature, not yet
 * built, distinct from what BR-ADM-01 to 08 individually ask for.
 *
 * <p><strong>BR-ADM-07's audit log is the one piece of state this module genuinely owns</strong> —
 * {@code web.AuditLog}, a slot in the OPEN foundational module, real-implemented by {@code
 * AuditLogService} here. Wired into three call sites this phase: vendor application approval and
 * rejection ({@code vendor.internal.VendorApplicationService}, previously unaudited beyond its own
 * domain events), catalog moderation ({@code catalog.internal.CatalogService.moderateDelist}, new
 * this phase), and commission configuration changes ({@code pricing.internal.PricingService}, also
 * new this phase). Every other admin action already built ahead of this module — vendor
 * suspension, dispute resolution, review removal, tax exception overrides, notification
 * preference changes, restriction lifts — is not yet wired to it; each already has its own local
 * record (a domain event, an {@code integrity_event} row, and so on) but not this shared one. That
 * is real, scoped-down work for a future pass, not a gap this module's own action list leaves
 * silently unaddressed.
 *
 * <p><strong>BR-ADM-02's "by phone" dimension is not built.</strong> {@code order} does not persist
 * the buyer's phone number on a {@code ParentOrder} — checkout captures it only to hand to {@code
 * payment} and {@code notification} — and resolving a phone number to an account id would need
 * {@code iam}, which is not in this module's dependency list and would be added opportunistically
 * rather than because a concrete piece of already-modelled data forced it. Search by reference, by
 * buyer account id, and by payment reference are real; phone is deferred, the same kind of honestly
 * recorded gap as {@code search}'s category filter.
 *
 * <p><strong>BR-ADM-06 is now fully real, closing a deferred gap.</strong> Delivery zones are
 * already real, admin-configurable state in {@code fulfilment} (not duplicated here, per the note
 * above); commission rate and holdback rate both have real create-a-new-version endpoints on
 * {@code AdminConfigController}, backed by {@code pricing.PricingAdministration} — holdback was
 * previously a bare {@code BigDecimal("0.10")} constant inside {@code
 * ledger.internal.LedgerService}, not versioned or configurable; see {@code
 * pricing.package-info.java} for how it now follows the exact versioning discipline commission
 * rate already established (COM-05, extended by BRD section 15.2 to holdback explicitly). The
 * settlement cycle is real too, but deliberately lives in {@code settlement} rather than here or
 * in {@code pricing} — {@code POST /settlement/config/cycle}, backed by {@code
 * SettlementService.createCycleConfiguration} — because unlike commission and holdback it is not
 * a per-order fact to reproduce, but a scheduling policy evaluated at settlement-run time; see
 * {@code settlement.SettlementCycle}'s javadoc for the full reasoning. This retrofit reached
 * {@code order}, {@code returns} and {@code ledger} (to thread the resolved holdback rate through
 * the same event-carried path commission rate already used) and {@code settlement} (for the
 * cycle), exactly the deliberate, non-rushed pass this note used to say it would take.
 *
 * <p><strong>BR-ADM-08 (impersonation) is now real, built in its own deliberate pass rather than
 * folded into this one.</strong> It was the one Could-priority requirement in BR-ADM — the rest
 * are Must — and the one flagged as actively worse half-built than absent, since "logged and
 * visible to the vendor" is load-bearing, not a nice-to-have on top of the mechanism. Both halves
 * are real: {@code iam.ImpersonationSessions} mints a genuine, short-lived (30-minute, not the
 * ordinary 30-day) session distinguishable from a real login by an {@code impersonatorAccountId}
 * on the row itself; {@code POST /admin/vendors/{vendorId}/impersonate}
 * ({@code vendor.internal.VendorImpersonationController}) is the admin action, logged both to this
 * module's own {@code AuditLog} and to {@code iam}'s auth-event trail; {@code GET
 * /vendors/{vendorId}/impersonation-log}, owner-gated, is what makes it visible to the vendor. It
 * does not live here, in {@code admin} — see {@code vendor.package-info.java} for why {@code
 * vendor} is the module that resolves "vendor account" into an actual account id, and {@code
 * iam.ImpersonationSessions}'s own javadoc for what this pass deliberately does not build
 * (per-request attribution of actions taken through an impersonation session back to the admin).
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Administration",
        allowedDependencies = {"order", "payment", "pricing", "web"})
package com.jamlapps.meridian.admin;
