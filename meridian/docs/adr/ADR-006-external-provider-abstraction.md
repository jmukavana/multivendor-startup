# ADR-006 — External providers behind internal interfaces

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 0 — Foundations
- **Satisfies:** INT-01 to INT-07, BR-FUL-03, RSK-04, RSK-09, RSK-13

## Context

BRD Section 13 requires every integration to sit behind an internal interface such that the
provider can be substituted without changes to business logic, and every outbound call to implement
timeout, retry with backoff, and circuit breaking.

The commercial risk is concrete rather than theoretical. OPEN-03 records that the payment
aggregator is not selected and two candidates are to be qualified. RSK-04 rates provider withdrawal
of split-payment capability as a High-impact risk. OPEN-08 records that no logistics partner is
selected. The system must be buildable before either decision is made, and survivable if either is
reversed.

## Decision

Each external dependency is represented by an interface owned by the consuming module, expressed in
that module's own vocabulary, with provider-specific implementations in an `internal` sub-package.

- `payment` owns the collection and disbursement interfaces. No provider type — no M-Pesa response
  object, no aggregator SDK class — appears outside `payment.internal`.
- `fulfilment` owns rate quotation, dispatch and tracking. BR-FUL-03 requires carriers to be added
  without changes to order processing, so this is a requirement rather than a preference.
- `tax` owns eTIMS transmission. `notification` owns SMS and email.

Resilience is applied at the adapter, not at the call site. `spring-cloud-starter-circuitbreaker-
resilience4j` is already a dependency; timeout, retry and circuit breaker are configured per
adapter so business code never sees a provider timeout as a special case.

Provider identifiers are stored (BR-PAY-12) but never interpreted outside the owning module.

## Consequences

**We get** the ability to build and test payments before OPEN-03 closes, using an in-memory adapter;
a second aggregator qualified under RSK-04 as an added implementation rather than a rewrite; and
carrier addition under BR-FUL-03 as configuration.

**We accept** an abstraction over things not yet chosen, which risks fitting the interface to the
first provider seen. The mitigation is to write the interface from the BRD's vocabulary — collect,
confirm, refund, disburse — before reading any provider's documentation.

**We accept** a translation layer whose cost is real and whose benefit only appears if a provider is
actually replaced. Given RSK-04's High rating and that neither provider is selected, that is a
reasonable bet.

**We must** not let idempotency leak. BR-PAY-05 and BR-STL-12 require idempotent callbacks and
disbursements. Whether the *provider* supports an idempotency key is a selection criterion under
OPEN-03; if it does not, the adapter must reconcile before retry. Either way the guarantee is the
interface's, not the provider's, and the modules above it must not need to know which.
