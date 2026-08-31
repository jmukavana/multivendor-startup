# ADR-001 — Modular monolith on Spring Modulith

- **Status:** Accepted
- **Date:** 31 August 2026
- **Phase:** 0 — Foundations
- **Satisfies:** BRD 1, NFR-14, NFR-04

## Context

The BRD describes eighteen functional areas that must ship over nine phases with a team of three
to four engineers and a fixed runway (BRD 5.5, 5.8). Two forces pull in opposite directions.

Microservices would give independent deployability, which nothing in the BRD asks for, at the cost
of distributed transactions across exactly the boundaries where correctness matters most: an order,
its payment, and its ledger entries. BR-LDG-03 requires ledger entries to be written atomically
with the transaction that causes them. That is a local transaction in a monolith and a saga in a
distributed system, and a saga is the wrong first thing to build with this budget.

A conventional layered monolith would give the transaction but not the boundary. On past evidence
the ledger and settlement code would grow direct references into catalog and vendor internals, and
by the time that becomes painful it is unaffordable to fix.

## Decision

Build a modular monolith using Spring Modulith. One deployable unit, one database, one transaction
manager. Module boundaries are declared in `package-info.java` and verified by `ModularityTests`,
which fails the build on violation.

Each module is a package directly beneath `com.jamlapps.meridian`. Types in the module's root
package are its public API; everything in a sub-package is internal and unreachable from other
modules.

## Consequences

**We get** a single local transaction spanning order, payment and ledger writes; refactoring across
modules with compiler support; boundaries that are enforced rather than documented; and the option
to extract a module into a service later, because the boundary already exists.

**We accept** a single deployment unit, so a change to search redeploys settlement; a single
runtime, so a memory leak anywhere degrades everything; and shared database capacity across all
modules. NFR-04's target of 10,000 concurrent buyers is reachable this way, but the scaling unit is
the whole application.

**We must** keep `ModularityTests` in the default build. If it is ever quarantined to make a
release, the architecture in ADR-002 stops existing that day.

## Alternatives considered

**Microservices from the start.** Rejected: it prices in distributed-transaction complexity to buy
independent deployability that the BRD never asks for.

**Layered monolith without enforcement.** Rejected: NFR-14 requires boundaries enforced
automatically in the build pipeline, and a convention no tool checks is not a boundary.
