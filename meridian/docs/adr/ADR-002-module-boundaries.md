# ADR-002 — Module boundaries and the dependency graph

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 0 — Foundations
- **Satisfies:** BRD 10, NFR-14, REG-17

## Context

BRD Section 10 groups requirements into fifteen functional modules. Those groupings were written
for readability, not as an architecture, and two of them do not survive contact with the code:
Section 14.2 defines tax invoicing with no `BR-` block of its own, and the versioned configuration
behind COM-05 and DAT-07 belongs to no section at all — BR-ADM-06 puts the *screen* in the
back-office while the *data* is read by checkout, order, ledger and settlement.

## Decision

Eighteen modules plus a shared kernel. Each declares the modules it may depend on; anything not
declared is a build failure.

| Module | BRD | Phase | May depend on |
|---|---|---|---|
| `shared` | DAT-01 | 0 | — (open module, available to all) |
| `iam` | BR-IAM | 1 | — |
| `pricing` | COM-01 to 05, DAT-07/08 | 0, 3 | — |
| `payment` | BR-PAY | 3 | — |
| `vendor` | BR-VND | 2 | `iam` |
| `catalog` | BR-CAT | 1, 2 | `vendor` |
| `inventory` | BR-INV | 2 | `catalog` |
| `order` | BR-ORD | 1, 4 | `catalog`, `vendor`, `pricing`, `payment::events` |
| `checkout` | BR-CHK | 4 | `catalog`, `inventory`, `vendor`, `pricing`, `fulfilment`, `order`, `payment` |
| `fulfilment` | BR-FUL | 7 | `vendor`, `order::events` |
| `ledger` | BR-LDG | 3 | `payment::events`, `order::events`, `returns::events` |
| `returns` | BR-RTN | 5 | `order`, `payment` |
| `settlement` | BR-STL | 6 | `ledger`, `vendor`, `order`, `returns`, `pricing` |
| `tax` | REG-04 to 08 | 3 | `order::events` |
| `review` | BR-REV | 8 | `order`, `catalog` |
| `search` | BR-SCH | 8 | `catalog` |
| `notification` | BR-NTF | 2+ | events of `iam`, `order`, `payment`, `settlement`, `returns`, `vendor` |
| `integrity` | BR-SEC | 9 | `iam` |
| `admin` | BR-ADM | 2+ | `vendor`, `catalog`, `order`, `payment`, `ledger`, `settlement`, `returns`, `pricing`, `integrity` |

Three rules shape the graph.

**Money flows one way.** `payment` depends on nothing. It collects against an opaque reference and
publishes what happened. `order` listens; `ledger` listens. Nothing reaches back into payment to
ask what an order means, so the payment integration can be replaced under RSK-04 without touching
order or ledger logic.

**`ledger` depends on no module that depends on it.** It consumes events from `payment`, `order`
and `returns` and exposes an API that `settlement` calls. Deliberately it does *not* listen to
`settlement` events, which would form a cycle and, more importantly, would make the ledger's
correctness depend on the correctness of the thing it is meant to check.

**`admin` is a sink and `notification` is a leaf.** Both depend on many modules; nothing depends on
either. Back-office breadth is therefore contained in one place instead of scattering admin
concerns through every module.

## Consequences

**We get** a graph where the four modules that handle money — `payment`, `ledger`, `order`,
`settlement` — sit at or near the bottom, so a change in search or reviews cannot reach them.
REG-17's vendor data segregation has an enforced structural component rather than resting on query
discipline alone.

**We accept** that `checkout` is an orchestrator with seven dependencies. This is the honest shape
of UC-03: a multi-vendor checkout genuinely has to consult catalog, inventory, vendor status,
pricing and delivery rates before it can create an order. Concentrating that in `checkout` keeps it
out of `order`, which stays a state machine.

**We must** treat a change to this table as an architectural decision. Adding a dependency means
editing a `package-info.java` and superseding this ADR, which is the point: the friction is the
control that BRD RSK-12 asks for.

## Note added in Phase 1: `iam`

`iam` was built before this table's Phase 1/2 modules and turned out to need nothing from the
graph — it depends on no other module, matching the original plan. What the plan did not foresee
was `notification` needing `iam::events`: one-time codes are delivered by SMS, which is
`notification`'s job, and `iam` publishes `OneTimeCodeIssued` rather than sending anything itself,
per ADR-004. `notification`'s allowed dependencies gained `iam::events` for that reason.

**Superseded in the deferred-gaps closing pass.** `iam::events` and `OneTimeCodeIssued` are gone;
`notification`'s dependency list lost that entry. One-time code delivery goes through
`web.MessageGateway` instead — a synchronous slot in `web`, called directly by `iam.internal
.AuthenticationService`, filled by `notification.internal.MessageGatewayAdapter` — see ADR-004's
own "Note added" section for why this one case is a synchronous call rather than an event, and
`iam`'s package doc for the bug that made the change necessary: the event never carried the
plaintext code, and nothing could recover one from the BCrypt hash it was hashed into, so no SMS
ever actually delivered a working code. `iam` still depends on nothing but `web`; the direction of
the new dependency is the same as every other foundational-module slot in this codebase
(`RateLimiter`, `AuditLog`) — a module below calls a module above through an interface the module
above defines, never the reverse.

`web` also gained a wiring detail worth recording here rather than only in ADR-004: `iam`
contributes its session-resolving filter to the security chain declared in `web`, but `web` never
imports an `iam` type to do it. The filter is injected into `web`'s configuration by Spring bean
name, typed only as `jakarta.servlet.Filter` — a JDK type. `web` stays the module everything else
depends on, not the reverse.

## Note on `pricing`

`pricing` owns the Commission Rate and Configuration Version entities added in BRD 15.1, and holds
the parameters that determine an amount owed: commission rates, holdback percentage, settlement
cycle, return window. It depends on nothing, because everything depends on it. BR-ADM-06's
management screens live in `admin` and call into it. See ADR-005.
