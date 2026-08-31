# ADR-005 — Versioned configuration for money-determining parameters

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 0 — Foundations, enforced from Phase 3
- **Satisfies:** COM-01 to COM-05, DAT-07, DAT-08, BR-ADM-06, AC-STL-08

## Context

COM-02 forbids a rate change applying retrospectively to orders already placed. COM-05 requires the
system to reproduce, for any historical order, the exact rate applied and the configuration version
that produced it. AC-STL-08 tests it: an order placed before a rate change and settled after it
must settle at the old rate, reproducibly.

A configuration table updated in place cannot satisfy COM-05 at all. Once a rate is overwritten,
the evidence of what a historical order should have been charged is gone, and BRD 5.2's precedence
chain — product override, then vendor rate, then category rate, then platform default — means the
answer depends on four rows that may each have changed independently.

The same applies to the parameters under BR-ADM-06: holdback percentage, settlement cycle and
return window each determine an amount owed or when it is owed.

## Decision

Parameters that determine an amount owed are versioned entities in the `pricing` module, never
mutable settings. Specifically: commission rates at all four precedence levels, holdback
percentage, settlement cycle and return window.

Each version carries an effective-from timestamp and is immutable once any order references it
(DAT-08). Changing a rate appends a version; it never updates a row.

**Every order records the configuration version identifier in force at its creation** (DAT-08).
Settlement, refund and commission reversal resolve rates through that stored identifier, never
through "current" configuration.

Delivery rate cards under BR-FUL-08 follow the same pattern but are owned by `fulfilment`, because
they are carrier data rather than platform pricing.

## Consequences

**We get** COM-02 and COM-05 by construction rather than by discipline. A commission reversal on a
refund six weeks later (BR-RTN-05) reverses the rate that was actually charged. A rate change is
inherently auditable, satisfying COM-04 with no separate audit path.

**We accept** that no code may read "the current commission rate" when computing anything against
an existing order. That is easy to write and wrong, and it is the most likely way this decision
gets violated. It is a review checklist item, and AC-STL-08 is the test that catches it.

**We accept** more rows and an extra join on the hot checkout path. Rate resolution is a candidate
for the Caffeine cache already configured, keyed by configuration version — which is safe to cache
aggressively precisely because versions are immutable.

**We must** design the configuration version as a single coherent snapshot rather than per-parameter
versioning. An order references one version identifier and gets a consistent set of parameters. Per-
parameter versioning would make "which configuration produced this order" unanswerable, which is
the question COM-05 asks.
