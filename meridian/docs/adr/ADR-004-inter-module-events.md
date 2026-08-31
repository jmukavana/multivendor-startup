# ADR-004 — Inter-module communication by domain event

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 0 — Foundations
- **Satisfies:** BR-ORD-08, BR-PAY-05, REG-05, NFR-06

## Context

A confirmed payment must, per the BRD, deduct stock (BR-INV-05), move sub-orders to Confirmed
(BR-ORD-03), emit a tax invoice (REG-04), notify buyer and vendor (BR-ORD-08, BR-NTF-01) and post
ledger entries (BR-LDG-02). Wiring those as direct calls from `payment` would make the payment
module depend on five others and would mean an SMS gateway timeout could roll back a confirmed
payment.

REG-05 separately requires invoice transmission to be a durable, retrying process with a
dead-letter queue, and NFR-06 requires that provider unavailability cause no data loss.

## Decision

Modules notify each other by publishing domain events. Consumers annotate with
`@ApplicationModuleListener`, which is transactional and runs after the publishing transaction
commits.

Event types live in an `events` sub-package exposed as a Spring Modulith named interface, so a
consumer may depend on `order::events` without gaining access to the rest of `order`.

Direct module dependencies are reserved for synchronous queries — `settlement` asking `ledger` for
a balance. Never for notification.

`spring-modulith-starter-jpa` is on the classpath, so event publications are persisted in the
event publication registry. An event whose listener fails is retained and can be resubmitted;
this is the durable, retrying mechanism REG-05 requires, and it works the same way for settlement
notifications and invoice transmission.

## Consequences

**We get** a `payment` module that depends on nothing; failure isolation, since a notification
failure cannot roll back a payment; and durability across restarts, because an incomplete event
publication survives in the database.

**We accept** eventual consistency between the publishing transaction and its consumers. A
confirmed payment and its tax invoice are not atomic. This is correct — REG-05 explicitly
contemplates retry and a dead-letter queue, so the invoice was never going to be atomic with the
payment — but it means UI and reports must not assume an invoice exists the instant a payment
confirms.

**We accept** that debugging spans a publication table rather than a stack trace. NFR-11's tracing
requirement is what makes this workable and is not optional.

**We must** add the event publication registry schema as a Flyway migration before Phase 3, not
rely on schema auto-generation. It is a production table carrying unprocessed business events.

## Boundary

Events are for notification, not for querying. A consumer that needs to ask a question calls the
owning module's API. A listener that calls back into the publishing module synchronously
re-creates the coupling this decision removes.
