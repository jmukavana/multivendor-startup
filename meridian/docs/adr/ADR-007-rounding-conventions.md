# ADR-007 — Rounding conventions for money

- **Status:** Proposed — requires Finance and Tax Advisor sign-off before Phase 3
- **Date:** 31 August 2026
- **Phase:** blocks Phase 3
- **Satisfies:** DAT-01, BR-LDG-05, REG-02, COM-05

## Context

ADR-003 deliberately gave `Money.percentage` no default rounding mode, on the grounds that
commission, VAT and holdback round differently and a default would silently pick one. That decision
is only complete once the conventions themselves are recorded. Until then every call site is an
independent guess, which is the situation ADR-003 was meant to prevent.

The amounts are small and the stakes are not. A single cent per order, in a consistent direction,
is roughly KES 1,569 a year at Year 1 volumes and KES 32,000 by Year 3 — immaterial as money and
fatal as a discrepancy, because BR-LDG-05 requires the trial balance to reconcile to zero and
BR-LDG-07 raises an alert on any variance against the provider balance. A systematic rounding
difference is not a rounding difference; it is a daily alert that trains everyone to ignore alerts.

**This is a business and tax decision, not an engineering one.** What follows is a proposal, with
the reasoning, for Finance and the Tax Advisor to accept or change.

## Proposed decision

| Amount | Rounding | Rationale |
|---|---|---|
| Commission (BR-STL-01) | `HALF_UP` on the item value | Conventional for invoiced amounts; predictable for a vendor checking the arithmetic by hand, which vendors will do |
| VAT (REG-02) | Per the tax authority's rule, **to be confirmed under DEP-02** | Not ours to choose. Placeholder `HALF_UP` until confirmed; if KRA specifies otherwise, that governs |
| Order processing fee | None — a fixed minor-unit amount | Not a percentage. Stated here so nobody applies a rate to it |
| Holdback (5.9) | `DOWN`, in the vendor's favour | Holdback is the vendor's money held temporarily. Rounding a withholding up is taking marginally more than owed, which is hard to defend and cheap to avoid |
| Delivery cost apportionment | `HALF_UP`, remainder to the largest sub-order | Splitting one delivery charge across sub-orders must not lose or invent a cent |
| Refunds and commission reversal (BR-RTN-05) | **Not rounded — reverse the stored figure** | The single most important row. A refund reverses the commission actually charged, read from the settlement line. Recomputing a percentage of a refunded amount produces a figure that differs from what was taken, and the difference never reconciles |

## The rule that matters

**Reversals reverse recorded amounts. They do not recompute them.**

A partial refund reverses the proportion of the *stored* commission, with the remainder assigned so
that the reversals of a fully-refunded order sum exactly to the original charge. Any approach that
re-derives commission from the refunded value will, on some orders, differ by a cent from what was
charged — and BR-LDG-04 forbids editing the original entry to match.

## Consequences

**We get** a stated convention per use, testable at the point of application, and a reversal rule
that keeps BR-LDG-05 satisfiable.

**We accept** that the VAT row is open until DEP-02 closes, so Phase 3 cannot finalise invoicing
before then — which the BRD already requires through REG-01.

**We must** apply these at the boundary where the amount is first computed and stored, never
re-derived later. `Money.percentage` requiring an explicit mode is the mechanism that makes a
missing decision visible instead of silently defaulted.

## Open

- VAT rounding awaits the tax opinion (DEP-02, OPEN-02).
- Whether commission rounds in the platform's or the vendor's favour is a commercial signal as much
  as an arithmetic one. `HALF_UP` is proposed as neutral; Finance may prefer `DOWN` as a vendor-
  friendly stance worth more in acquisition than it costs in revenue.
