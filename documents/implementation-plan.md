---
title: "Project Meridian — Implementation Plan"
subtitle: "Backend and frontend delivery, phases 0 to 9"
date: "31 August 2026"
lang: en-GB
---

## Purpose

How the BRD v2.2 scope gets built, in what order, across two codebases. It is subordinate to the
BRD: where they disagree, the BRD wins and this document is wrong. Phase numbering follows BRD
Section 18 as corrected in v2.2, where returns precede settlement and hardening precedes launch.

**Repositories**

| Path | Stack | Role |
|---|---|---|
| `meridian/` | Java 25, Spring Boot 4.1, Spring Modulith 2.1, PostgreSQL, Flyway | Backend — all business logic, the system of record |
| `meridian-frontend/` | Next.js 16.3, React 19.2, Tailwind 4, Biome, Zod | Buyer storefront, vendor dashboard, admin back-office |

---

## 1. Where the two codebases actually are

This is the single most important fact in this plan, and it is not what the phase numbering
implies.

**Backend is at Phase 0.** Eighteen modules plus a shared kernel exist as enforced package
boundaries. `Money`, the event publication registry migration, and the modularity and boundary
tests are in place. There are **no entities, no endpoints, no security configuration, and no
persistence beyond the event registry**. Nothing can be called.

**Frontend has built part of Phase 2.** `src/features/vendor-onboarding` implements the vendor
application form, server actions, Zod schemas and the submitted-state page, referencing UC-01 and
BR-VND-01/02/04/05 directly. `src/shared/api/client.ts` is a considered HTTP client with bearer
auth, idempotency keys, request tags, timeouts and typed errors. `src/shared/lib/session.ts`
expects an httpOnly `meridian_session` cookie issued by the backend.

**The gap is a contract nobody agreed.** `src/shared/api/types.ts` defines a response envelope —
`{ data, meta }` with `traceId`, `generatedAt`, and a `pagination` / `sort` / `appliedFilters`
shape for collections — that the backend has never seen and does not implement. The frontend also
assumes a session cookie the backend does not issue, and an error-code vocabulary
(`API_ERROR_CODES`) the backend does not emit.

None of that frontend work is wasted; the envelope is a reasonable design. But it is currently a
one-sided contract, and every week it stays one-sided is more frontend code written against
assumptions that may not survive contact with the backend.

**Consequence for sequencing.** Phase 1's first deliverable is not a feature. It is the contract:
the envelope, the error vocabulary, the auth mechanism and an OpenAPI document generated from the
backend, adopted by the frontend. Everything else in Phase 1 follows from it.

---

## 2. Ground rules

**Contract first, generated not hand-written.** `springdoc-openapi` is already a backend
dependency. The backend publishes `/v3/api-docs`; the frontend generates its request and response
types from that document. A hand-maintained parallel type definition is how the two drift.

**The frontend holds no business rules.** Zod schemas validate shape and give fast feedback. They
are not the authority on anything: commission, VAT, delivery cost, stock availability and vendor
eligibility are decided by the backend and only displayed by the frontend. Where a rule appears in
both, the frontend's copy is a convenience and must fail closed if the backend disagrees.

**Money crosses the wire as minor units and a currency code**, never as a formatted string or a
float, matching DAT-01 and `Money` on the backend. The frontend formats for display at the edge.

**Every mutating endpoint is idempotent.** The frontend client already sends an idempotency key;
the backend must honour it. This is BR-PAY-05 and BR-STL-12 generalised, and it is much cheaper to
adopt as a convention now than to retrofit onto payments alone.

**Definition of done, per requirement:** backend logic with tests; the acceptance criteria from BRD
Section 11 that trace to it, automated; the OpenAPI document updated; the frontend consuming it; a
happy path and one failure path exercised end to end.

**The build stays green.** `./gradlew test` runs `ModularityTests` and `BoundaryEnforcementTests`.
Quarantining either to make a release ends the architecture in ADR-002 that day.

---

## 3. Phase plan

Each phase lists the two workstreams, what must be true to start, and the BRD Section 11.11 exit
criterion. Phases are sequential; the workstreams inside them are parallel.

### Phase 0 — Foundations

*Exit (BRD 11.11): pipeline deploys to staging automatically; AC-NFR-03 passes; ADRs approved.*

| | Backend | Frontend |
|---|---|---|
| Done | Module skeleton with enforced boundaries; `Money`; event registry migration; ADR-001 to 006; AC-NFR-03 automated | Project scaffold, Tailwind 4, Biome, app shell |
| Remaining | Observability per NFR-11 — structured JSON logging, Micrometer metrics, OTel tracing; container image; staging manifests | Error boundary and loading conventions; base layout and design tokens |
| Shared | CI pipeline running both builds; staging environment; ODPC and company registration (DEP-01, DEP-05) | |

**Blocked on:** a git remote. There is none, so no pipeline can run. **OPEN-02 must close before
Phase 0 exits** — the VAT classification decides pricing, and DEP-02 is due here.

### Phase 1 — Walking skeleton and the API contract

*Exit: one order completes end to end in staging; observability visible for that order.*

| Backend | Frontend |
|---|---|
| Response envelope, error vocabulary, trace propagation — the shape `shared/api/types.ts` already assumes | Regenerate types from `/v3/api-docs`; reconcile with the hand-written envelope |
| Session auth: issue the httpOnly `meridian_session` cookie the frontend expects | Replace assumptions in `session.ts` with the real mechanism |
| `iam`: registration by phone or email, OTP verification (BR-IAM-01/02), RBAC skeleton (BR-IAM-03) | Sign-in, OTP entry, session handling |
| `catalog`: minimal product read | Product listing and detail, minimal |
| `order`: parent and sub-order creation in Pending Payment, per BR-ORD-01 and the v2.2 flow | Order confirmation view |
| Payment marked manually — no provider yet | |

**This phase is where the contract stops being one-sided.** Do it first, before any further
feature work on either side.

### Phase 2 — Vendor lifecycle

*Exit: UC-01 and UC-02 pass in full including exception flows; AC-VND-01 to 06 and AC-CAT-01 to 04 pass.*

| Backend | Frontend |
|---|---|
| `vendor`: application state machine (BR-VND-05), admin approval queue, document upload, payout details with cooling-off (BR-VND-08/09) | **Reconcile the existing `vendor-onboarding` feature against the real API** |
| `catalog`: product CRUD, variants, categories, moderation states, price history | Vendor dashboard: product create and edit, image upload |
| `inventory`: stock, reservations, movements | Stock management screens |
| `notification`: SMS adapter, transactional sends (BR-NTF-01/02) | Application status views |
| `admin`: approval queue (BR-ADM-01) | Admin approval screens |

**Blocked on:** OPEN-01 (category) gates the catalog schema per ASM-05; OPEN-07 (prohibited items)
gates BR-CAT-08; DEP-07 (SMS gateway) gates notifications.

### Phase 3 — Money

*Exit: all Critical criteria in 11.5 and 11.6 plus AC-REG-01 to 03 pass; trial balance reconciles across a 1,000-order synthetic load.*

| Backend | Frontend |
|---|---|
| `pricing`: versioned commission and configuration per ADR-005 and DAT-07/08 | Price and total displays sourced entirely from backend figures |
| `payment`: aggregator adapter behind the ADR-006 interface, M-Pesa STK Push, idempotent callbacks (BR-PAY-05), status polling (BR-PAY-06), daily reconciliation | Checkout payment step, STK prompt waiting state, timeout and failure paths |
| `ledger`: double-entry, atomic posting (BR-LDG-03), trial balance | — |
| `tax`: eTIMS invoice emission with durable retry and dead-letter (REG-05) | Invoice display and download |

**The heaviest phase.** Do not compress it. **Blocked on:** OPEN-03 and DEP-03 (aggregator),
DEP-04 (eTIMS). Build against an in-memory adapter until they close — that is what ADR-006 is for.

### Phase 4 — Multi-vendor mechanics

*Exit: UC-03 passes in full; AC-ORD-01 to 05 pass.*

| Backend | Frontend |
|---|---|
| `checkout`: multi-vendor cart, per-vendor delivery selection, revalidation at checkout (BR-CHK-04) | Cart grouped by vendor with per-group delivery, full cost breakdown (BR-CHK-07) |
| `order`: splitting, per-sub-order state, partial cancellation and refund, **buyer cancellation (BR-ORD-13 to 16)** | Order history with per-vendor status; cancel affordance that disappears at Dispatched (BR-ORD-15) |

### Phase 5 — Returns, refunds and disputes

*Exit: UC-06 and UC-07 pass in full; AC-RTN-01 to 05 pass.*

Moved ahead of settlement in v2.2 — settlement cannot be exercised without the return window.

| Backend | Frontend |
|---|---|
| `returns`: return window, reasons with images, vendor acceptance, escalation, commission reversal (BR-RTN-05) | Buyer return request flow with image upload; vendor accept/reject |
| `admin`: dispute queue with full order, payment and communication history | Admin dispute resolution screens |

**Blocked on:** OPEN-05 (return window duration).

### Phase 6 — Settlement

*Exit: UC-05 passes in full; a settlement run executes end to end with finance approval and reconciles.*

| Backend | Frontend |
|---|---|
| `settlement`: entitlement calculation, holdback release, runs, statements, **idempotent disbursement (BR-STL-12)**, finance approval gate (BR-STL-11) | Vendor statement and earnings views |
| Recovery of refunds against future settlements (BR-RTN-08) | Finance console: pre-run report, approval, exception handling |

**Blocked on:** OPEN-04 (settlement cycle), OPEN-05 (holdback), DEP-09 (pilot vendors).
**AC-STL-08 is the phase's hardest test** — an order placed before a rate change must settle at the
old rate, reproducibly.

### Phase 7 — Fulfilment

*Exit: UC-04 passes against a live carrier in production configuration.*

| Backend | Frontend |
|---|---|
| `fulfilment`: carrier interface (BR-FUL-03), rate quotation, dispatch, tracking, pickup points as first-class (BR-FUL-02), zones and rate cards | Delivery method selection, pickup point picker, tracking view |

**Blocked on:** OPEN-08 and DEP-06 (logistics partner). Pickup point selection is a map-and-search
interface and is more frontend work than it appears.

### Phase 8 — Discovery

*Exit: UC-08 and UC-12 pass.*

| Backend | Frontend |
|---|---|
| `search`: keyword, filtering, sorting, exclusion of unavailable listings | Search UI, facets, result pages |
| `review`: reviews restricted to delivered orders, aggregation, moderation | Review submission and display |

Reviews are Should in v2.2, not Must. If the phase is under pressure, search ships and reviews slip.

### Phase 9 — Hardening — **launch gate**

*Exit: all Critical criteria across Section 11 pass; AC-NFR-04 verified by an actual restore.*

| Backend | Frontend |
|---|---|
| `integrity`: BR-SEC-01 to 08 — rate limiting, abuse signals, reversible restrictions | Rate-limit and lockout states |
| Load test to NFR-04; restore drill to NFR-13 | Lighthouse and axe to NFR-01 and NFR-15 (WCAG 2.1 AA) |
| Penetration test; NFR-12 alerting | PWA installability per NFR-05 |

Rate limiting (BR-SEC-01/02) is pulled forward to Phase 1 alongside the authentication it protects.

---

## 4. Cross-cutting tracks

These run continuously, not as phases.

| Track | Backend | Frontend |
|---|---|---|
| **Observability** (NFR-11) | Structured JSON logs, Micrometer metrics, OTel traces, module-level spans via `spring-modulith-observability` | Web vitals, client error reporting, trace ID propagated from `meta.traceId` |
| **CI/CD** | `./gradlew test` including modularity and boundary tests, then image build | `biome check`, `tsc --noEmit`, `next build` |
| **Accessibility** (NFR-15) | — | axe in CI from Phase 1, not retrofitted at Phase 9 |
| **Data protection** (REG-12 to 24) | Erasure by severance per DAT-06, consent records, breach detection | Consent capture separated from transactional (REG-15) |
| **Performance** (NFR-01) | Query budgets, Caffeine on versioned configuration | 3G budget enforced in CI from Phase 2 |

---

## 5. Blocked-by summary

Nine of the eleven open issues gate a phase. Two gate Phase 0 exit and should be treated as urgent.

| Open issue | Gates | Owner |
|---|---|---|
| OPEN-02 VAT classification | Phase 0 exit — pricing model | Legal / Tax |
| OPEN-10 Platform-controlled account | Phase 0 exit — settlement design, and AC-REG-07 currently fails as drafted | Legal |
| OPEN-06 Who prices delivery | Phase 0 exit — input to OPEN-02 | Product / Tax |
| OPEN-11 Schedule re-estimation | Phase 0 exit — the 9-month figure excludes phases 5 to 9 | Product / Engineering |
| OPEN-01 Category | Phase 2 — catalog schema, commission bands | Product |
| OPEN-07 Prohibited items | Phase 2 | Legal |
| OPEN-03 Aggregator | Phase 3 | Sponsor |
| OPEN-04 Settlement cycle, OPEN-05 Holdback | Phases 5 and 6 | Finance |
| OPEN-08 Logistics partner | Phase 7 | Operations |
| OPEN-09 Product name | Phase 1 — buyer-facing build begins here | Sponsor |

---

## 6. Risks specific to delivery

**The frontend will keep outrunning the backend.** It already has, by three phases. Frontend work
is faster to produce and easier to produce speculatively, and every screen built against an
imagined API is rework waiting to happen. The contract work in Phase 1 is the control. A second
control: no frontend feature merges without the backend endpoint it calls existing in the OpenAPI
document.

**Phase 3 is where schedules die.** Payments, ledger, tax invoicing and reconciliation in one
phase, against two integrations that are not yet contracted. It carries more risk than phases 4
through 8 combined.

**The team is three to four engineers across two stacks.** Both workstreams in every phase assumes
parallel capacity that may not exist. If it does not, the frontend for a phase trails its backend
by a sprint, and the plan should be re-cut as a single stream rather than pretending otherwise.

**OPEN-09.** Every buyer-facing string, the domain, and the `MER-` order prefix in BR-ORD-01 are
placeholders. Renaming after Phase 1 makes historical order references a data migration.
