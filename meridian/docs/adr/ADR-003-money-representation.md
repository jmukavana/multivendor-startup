# ADR-003 — Money as minor units with explicit currency

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 0 — Foundations
- **Satisfies:** DAT-01, BR-LDG-05, COM-05

## Context

DAT-01 prohibits floating-point representation of money and requires an explicit currency on every
amount. BR-LDG-05 requires a trial balance that reconciles to zero at any point in time, which
fails the moment a rounding difference is absorbed silently rather than posted.

The specific hazard in this system is commission. BRD 5.3 computes 9% of an item value, which for
most amounts is not a whole number of cents. Where that fraction goes decides whether the buyer's
payment, the vendor's entitlement and the platform's revenue still sum to zero.

## Decision

A single `Money` type in the shared kernel: a `record` of `long minorUnits` and
`java.util.Currency`. Minor units means cents. `double`, `float` and bare `BigDecimal` must not
appear in a monetary field.

Arithmetic uses `Math.addExact` and friends, so an overflow throws rather than wrapping.

**`Money.percentage(rate, roundingMode)` takes an explicit rounding mode and has no default.**

## Consequences

**We get** exact arithmetic, an overflow that fails loudly, and a compile-time signal at every
place where money is rounded.

**We accept** that every call site applying a rate must state its rounding intent, which is more
verbose than a default would be. That verbosity is the point. Commission rounds in the platform's
favour or the vendor's; VAT under REG-02 rounds per the tax authority's rule, which is not
necessarily the same; holdback rounds against a percentage of an already-rounded figure. A single
default would silently pick one convention for all three, and the resulting discrepancies would
surface as a trial balance that does not reconcile — the exact failure BR-LDG-05 and RSK-05 exist
to prevent.

**We must** decide the rounding convention per use before Phase 3, and record it. The convention
itself is a business decision, not an engineering one: it moves money.

## Alternatives considered

**JSR-354 / Moneta.** Rejected for now: a substantial dependency for multi-currency support that
BRD 6.2 places out of scope. Revisit if cross-border sales enter scope.

**`BigDecimal` with a scale convention.** Rejected: it permits a fractional cent to exist in a
variable, and the value that reaches the ledger then depends on where rounding happened to occur.
