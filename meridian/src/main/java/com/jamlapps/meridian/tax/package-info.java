/**
 * Tax.
 *
 * <p>Implements REG-04 to 08 of the business requirements document. Delivered in phase 3.
 * Allowed dependencies: order::events, returns::events.
 *
 * <p>{@code returns::events} is a deliberate addition beyond ADR-002's table, which lists only
 * {@code order::events} here — matching the pattern {@code returns} and {@code order} themselves
 * already set with their own deviations. REG-08's credit notes are triggered by two different
 * refunds: {@code order.events.SubOrderCancelled} (BR-ORD-13/14, already covered by the table)
 * and {@code returns.events.ReturnRefunded} (BR-RTN-05, not covered until now) — both are needed
 * for the same requirement, so both are declared.
 *
 * <p>BRD 15.1 files Credit Note under the Returns module's entity table, but it lives here
 * instead, alongside Tax Invoice — the same reasoning 15.1 itself gives for filing Refund under
 * Payments rather than Returns: a credit note needs the tax-transmission infrastructure (REG-05/06/07's
 * retry, dead-letter and acknowledgement handling), not the returns-workflow infrastructure, and
 * it has to reference the invoice it corrects.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Tax",
        allowedDependencies = { "order::events", "returns::events" })
package com.jamlapps.meridian.tax;
