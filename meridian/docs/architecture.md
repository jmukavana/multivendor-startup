# Meridian — architecture

Phase 0 skeleton. Structure only: no module has behaviour yet.

## Shape

A modular monolith on Spring Boot 4.1 with Spring Modulith 2.1 (ADR-001). One deployable, one
database, one transaction manager. Eighteen modules plus a shared kernel, each a package directly
beneath `com.jamlapps.meridian`.

Types in a module's root package are its public API. Everything in a sub-package is internal and
unreachable from other modules. A module's `package-info.java` declares which other modules it may
depend on; anything undeclared fails the build.

## Enforcement

`ModularityTests` runs in the default build and fails on an undeclared dependency, on access to
another module's internals, or on a cycle. This is NFR-14.

    ./gradlew test --tests "*ModularityTests*"

It also writes PlantUML component diagrams and module descriptions to
`build/spring-modulith-docs/`.

The enforcement has been verified by introducing a deliberate violation — a class in `search`
referencing a type in `settlement` — and confirming the build failed with
`org.springframework.modulith.core.Violations`, then removing it.

## Modules

| Module | Owns | BRD | Phase | Built |
|---|---|---|---|---|
| `shared` | `Money`, `BlobStorage` (local-disk adapter), cross-cutting value types | DAT-01 | 0 | Yes |
| `web` | API envelope, error codes, problem details, OpenAPI, baseline security | — | 1 | Yes |
| `iam` | Registration, one-time codes, sessions, RBAC | BR-IAM | 1 | Yes |
| `pricing` | Commission rate, versioned configuration | COM-01 to 05, DAT-07/08 | 0, 3 | Yes |
| `vendor` | Application lifecycle, documents, payout, staff, suspension | BR-VND, BR-IAM-04 | 1, 2 | Yes |
| `catalog` | Products, images, publish/moderation, price history | BR-CAT, BR-SCH-01/04 | 1, 2 | Yes |
| `inventory` | Stock, reservations, movements | BR-INV | 2 | Yes |
| `order` | Parent/sub-order creation, confirmation, cancellation | BR-ORD-01/02/09/10/13 to 16 | 1, 4, 5 | Yes |
| `payment` | Collection, provider callback | BR-PAY | 3 | Yes |
| `ledger` | Double-entry ledger, trial balance | BR-LDG | 3 | Yes |
| `checkout` | Multi-vendor cart, order + reservation + payment orchestration | BR-CHK | 4 | Yes |
| `settlement` | Entitlement, settlement runs, disbursement | BR-STL | 6 | Yes, reduced — see below |
| `returns` | Return requests, disputes | BR-RTN | 5 | Yes, reduced — see below |
| `fulfilment` | Carriers, zones, rate cards, tracking | BR-FUL | 7 | Yes, reduced — see below |
| `tax` | eTIMS invoices and credit notes | REG-04 to 08 | 3 | Yes — see below |
| `review` | Reviews and ratings | BR-REV | 8 | Yes — see below |
| `search` | Keyword search, filtering, sorting | BR-SCH | 8 | Yes — see below |
| `notification` | SMS and email delivery, retry | BR-NTF | 2+, 9 | Yes, reduced — see below |
| `integrity` | Rate limiting, abuse and fraud signals | BR-SEC | 9 | Yes, reduced — see below |
| `admin` | Back-office queues and configuration screens | BR-ADM | 2+, 9 | Yes, reduced — see below |

The full dependency table is in ADR-002. Where a module's actual `allowedDependencies` diverges from
it — `settlement`'s is a subset (missing `order`, `returns`, `pricing`, none of them needed yet);
`returns`'s is a superset (`vendor`, added for BR-RTN-03's ownership check); `order`'s is a superset
too (the full `payment` module, added for BR-ORD-14's synchronous refund call on cancellation);
`tax`'s is a superset as well (`returns::events`, added for REG-08's return-driven credit notes);
`review`'s is a superset too (`vendor`, added for BR-REV-04/06's ownership checks); `notification`'s
is a superset too (`iam` and `vendor`, the base modules rather than just their `::events`
interfaces, added to resolve an account or vendor id to a real phone number or email); `integrity`'s
is a subset (empty — nothing built this phase needs `iam`) — each module's own `package-info.java`
explains its gap; ADR-002 stays the target architecture, not a record of what exists today.

## Rules

1. **Notify by event, query by call.** Cross-module notification uses domain events consumed with
   `@ApplicationModuleListener`. Direct dependencies are for synchronous questions only. ADR-004.
2. **Money is `Money`.** Minor units, explicit currency, explicit rounding at every call site.
   No `double`, no bare `BigDecimal` in a monetary field. ADR-003.
3. **Never read current configuration for an existing order.** Orders store the configuration
   version in force at creation and resolve rates through it. ADR-005.
4. **No provider type escapes its adapter.** External providers sit behind interfaces owned by the
   consuming module, in its vocabulary. ADR-006.
5. **Controllers return `ApiResponse.of(...)` and never build metadata.** The trace id and timestamp
   are stamped during response writing. Errors are thrown, not returned — `ApiException` carries the
   code the frontend branches on.

## The API contract

`web` is an open module registered as shared, so any module can return `ApiResponse` from a
controller without declaring a dependency on it.

- Success: `{ "data": ..., "meta": { "traceId", "generatedAt" } }`, with `pagination`, `sort` and
  `appliedFilters` added on collections.
- Errors: RFC 7807 problem details extended with `code`, `traceId`, `timestamp` and `fieldErrors`.
- Every `@RestController` is served under `/api`, matching `MERIDIAN_API_URL`. `/actuator` is
  unaffected.
- `/v3/api-docs` is the artefact the frontend generates its types from.

**The shape is not ours alone.** `meridian-frontend/src/shared/api` already consumes this envelope,
these error codes and this problem shape. `ApiContractTests` asserts the JSON rather than the Java
types, because a rename that compiles breaks the frontend at runtime and nowhere else.

Spring Security rejects requests in the filter chain, before the dispatcher, so its 401 and 403
never reach the controller advice. `ProblemFactory` is shared between both paths so the two cannot
diverge — otherwise the frontend would read `UNKNOWN_ERROR` on every authentication failure.

## Decisions

| ADR | Decision |
|---|---|
| [ADR-001](adr/ADR-001-modular-monolith.md) | Modular monolith on Spring Modulith |
| [ADR-002](adr/ADR-002-module-boundaries.md) | Module boundaries and the dependency graph |
| [ADR-003](adr/ADR-003-money-representation.md) | Money as minor units with explicit currency |
| [ADR-004](adr/ADR-004-inter-module-events.md) | Inter-module communication by domain event |
| [ADR-005](adr/ADR-005-versioned-configuration.md) | Versioned configuration for money-determining parameters |
| [ADR-006](adr/ADR-006-external-provider-abstraction.md) | External providers behind internal interfaces |
| [ADR-007](adr/ADR-007-rounding-conventions.md) | Rounding conventions for money — **proposed, awaiting Finance and Tax sign-off** |
| [ADR-008](adr/ADR-008-session-tokens.md) | Opaque, revocable session tokens rather than self-issued JWTs |

## Database

`V1__event_publication_registry.sql` creates the Spring Modulith event publication registry. It is
a production business table — an event whose listener has not completed is a row in it — so it is
created by migration rather than schema auto-generation (ADR-004).

Note that `MeridianApplicationTests.contextLoads` failed before this migration existed, and had
been failing since the project was generated: the registry bean is created whenever
`spring-modulith-starter-jpa` is on the classpath, `ddl-auto` defaults to none, and there were no
migrations.

## Observability

NFR-11 wiring is in place from the start rather than added when something is slow:

- **Structured logging** — ECS JSON on stdout, with a `local` profile that restores readable console
  output. Trace and span ids are correlated into every line.
- **Metrics** — Micrometer with a Prometheus registry at `/actuator/prometheus`, tagged with
  application and environment.
- **Tracing** — OpenTelemetry via `spring-boot-starter-opentelemetry`, OTLP export off unless
  `OTLP_TRACES_ENDPOINT` and `OTLP_TRACES_ENABLED` are set, so a missing collector cannot fail
  startup. `spring-modulith-observability` adds a span per module invocation, so a trace shows which
  module did what.

`ObservabilityTests` asserts the registry and tracer are actually wired. Observability configuration
is the kind that looks correct and does nothing — a renamed property or an autoconfiguration backing
off silently presents as a healthy application with no traces.

The trace id also reaches the frontend: it is the `meta.traceId` in the API response envelope, so a
buyer-reported failure can be followed from the browser to the module that failed.

## Build and deploy

| Artefact | Status |
|---|---|
| `Dockerfile` | Layered image from the boot jar. Built and run locally: 337 MB, non-root, ECS JSON logging |
| `deploy/staging/` | Namespace, config, deployment, service. YAML-parsed only — never applied, no cluster |
| `.github/workflows/` | Backend and frontend pipelines, path-filtered |

**No pipeline has ever run: the repository has no git remote.** See `.github/workflows/README.md`
and `deploy/staging/README.md` for what remains.

## Not yet done

- **Deployment.** No git remote, cluster or registry exists, so the second half of the Phase 0 exit
  criterion — "pipeline deploys to staging automatically" — is not met. Everything that can be
  written without infrastructure has been.
- **ADR-007 sign-off.** The rounding conventions are drafted but not accepted. Finance and the Tax
  Advisor own that decision, and the VAT row depends on DEP-02.
- **Migration strategy at more than one replica.** Flyway runs at application start and staging
  runs two replicas. See `deploy/staging/README.md`.

## Identity and access (BR-IAM-01, 02, 03, 05, 06)

`iam` implements registration by phone or email, one-time code verification, opaque session
tokens per ADR-008, and role-based access control. It depends on no other module.

**Sessions travel as a bearer token, not a cookie the backend manages.** The frontend's API client
is server-only — the browser calls the Next.js server, which calls this API. A `Set-Cookie` header
on that server-to-server response would not reach the browser; Node's `fetch` does not forward it.
So `/api/auth/verify` returns the token in the response body, and it is the frontend's job (not yet
built) to set the `meridian_session` cookie itself, in a Server Action, on its own response to the
browser. Every authenticated call to this API — sign-out included — reads `Authorization: Bearer
<token>`, exactly what `apiFetch` already sends.

**Two things worth knowing if you touch this module:**

- `SessionAuthenticationFilter` has no `@Transactional` anywhere in it, deliberately.
  `@Transactional` on a class extending `OncePerRequestFilter` forces a CGLIB proxy built through
  Objenesis, which bypasses every constructor in the hierarchy — including `GenericFilterBean`'s
  own field initializers, silently leaving `logger` null and throwing on the first request. The
  transactional work lives in `SessionResolver`, a plain `@Service`, instead.
- Role grants are stored with a `granted_at` timestamp (`account_role.granted_at`, `NOT NULL`),
  mapped via the `RoleGrant` embeddable rather than a bare `Set<Role>` — the audit trail BR-IAM-06
  asks for extends to when a role was granted, not only that it holds one.

`AuthenticationFlowTests` (in `iam.internal`, so it can retrieve a plaintext one-time code that
production code never exposes any other way) proves the full HTTP round trip, including that
revocation is immediate: reusing a token after `DELETE /api/auth/sessions` gets `SESSION_EXPIRED`
on the very next request, not eventually.

**`AuthenticatedCaller` lives in `web`, not `iam`.** The first authenticated controller outside
`iam` (`CatalogController`) revealed that `@AuthenticationPrincipal MeridianPrincipal` was the
wrong design: every module with an authenticated endpoint needs to know who is calling, and almost
none of them need `iam`'s full `Account` shape behind that identity. `web.AuthenticatedCaller`
carries only an account id and raw authority strings — no `iam` type, no dependency edge, matching
how the security filter itself was already wired without one. A module that needs the full account
asks `iam.Accounts` for it, the same read-only pattern every cross-module lookup here follows.
`MeridianPrincipal` was deleted; nothing used it once this shipped.

## Walking skeleton: `vendor`, `catalog`, `order` (Phase 1)

BRD 18's Phase 1 exit criterion — one order completes end to end in staging — needs a vendor, a
product and an order, none of which have their real BR-VND/BR-CAT/BR-ORD shape until Phases 2, 2
and 4. Three modules exist now as deliberate stand-ins, each documented in its own public API
javadoc as provisional:

- **`vendor`** — one table, no application or approval workflow. Any authenticated account may
  create one vendor for itself. BR-VND-07 ("an unapproved vendor may not publish or sell") is
  unenforced until Phase 2 makes "approved" a real state rather than the only state.
- **`catalog`** — one price, one currency, no variants, no categories, no moderation. Listing and
  detail are open to guests (BR-IAM-08); creating a product requires a vendor.
- **`order`** — BR-ORD-01/02: one sub-order (there is exactly one vendor per order until checkout,
  Phase 4, can split a cart), one line, an immutable event log (BR-ORD-10). Payment is marked
  manually via `POST /orders/{id}/mark-payment-confirmed`, standing in for Phase 3's real payment
  callback, and is the one endpoint in this phase secured with `@PreAuthorize` rather than a
  `SecurityConfiguration` URL rule — a finance action belongs guarded on the endpoint doing the
  sensitive thing, not synchronised by hand against a separate config class.

`WalkingSkeletonFlowTests` proves BRD 18's criterion directly and through real HTTP: a vendor
registers, lists a product; a buyer registers, orders it; an elevated account confirms payment; the
order and its sub-order both move to `CONFIRMED`. A second test proves the guard actually guards —
an ordinary buyer calling the confirmation endpoint gets `403 FORBIDDEN`, not silent success.

**`OrderPaymentConfirmed`** is published on confirmation (ADR-004) and consumed by a `notification`
listener the same way `OneTimeCodeIssued` is. At the time this phase was built, both were logged at
`INFO` rather than sent — DEP-07 had not provided an SMS gateway, and the Phase 1 vendor stand-in
captured no contact details for the vendor half of BR-NTF-01/02 to reach. Phase 9 replaces both
gaps with real delivery; see that section.


## Phase 2: vendor lifecycle and catalog moderation

BRD 18's Phase 2 exit criterion is AC-VND-01 to 06 and AC-CAT-01 to 04 passing. Both sets are now
built, tested through real HTTP in `WalkingSkeletonFlowTests` (8 tests), and green.

### Vendor lifecycle (`vendor`)

Phase 1's self-service `POST /vendors` is gone. A `Vendor` operational row now exists only because
a `VendorApplication` (BR-VND-01 to 06 state machine: Submitted, Under Review, Information
Requested, Approved, Rejected) was approved by an admin — `AdminVendorApplicationController`,
method-secured with `@PreAuthorize`, living in `vendor.internal` rather than a not-yet-built
`admin` module, the same choice made for `order`'s payment-confirmation endpoint in Phase 1.

**`VendorApplicationStatus` never stores `SUSPENDED`**, even though the frontend's
`VendorApplicationStatus` type includes it (matching BR-VND-05's literal wording). An application's
process ends at Approved or Rejected; suspension is the resulting `Vendor` row's own status.
`VendorApplicationEntity.toVendorApplication` computes the reported status as `SUSPENDED` when the
linked vendor is, so the API still returns exactly the value the frontend expects without this
entity tracking the same fact twice.

**BR-VND-10 (suspension) delists across a module boundary via an event, not a query.** `vendor`
publishes `VendorSuspended`; `catalog`'s `VendorSuspensionListener` bulk-delists that vendor's
active products. `@ApplicationModuleListener` is `@Async` by definition (confirmed from its own
source: `@Async @Transactional(REQUIRES_NEW) @TransactionalEventListener`), so "immediately" is a
business-requirement claim about how soon buyers stop seeing the vendor's listings, not a claim
that the suspend response and the delisting land in the same instant — `WalkingSkeletonFlowTests`
polls rather than asserting zero-latency consistency, to test the real contract.

**BR-IAM-04 / AC-VND-03** needed a relationship IAM does not model: `iam.Role.VENDOR_STAFF` is
global, but "is this account staff *for this vendor*" is per-vendor. A small `vendor_staff` table
(V7) answers it; `PayoutDetailController` returns 404 to an unrelated caller and 403 specifically to
a vendor's own staff, matching BR-IAM-04's wording, and logs every view/change attempt regardless of
outcome (`payout_detail_access_event`).

**Multi-tenancy is row-level, enforced in the WHERE clause, not schema-per-tenant.** BR-CAT-11's
vendor isolation and the equivalent buyer/account scoping elsewhere are a single-schema concern —
`ProductRepository.findByIdAndVendorId`, `ParentOrderRepository.findByIdAndBuyerAccountId`,
`VendorApplicationRepository.findByIdAndAccountId`, `VendorRepository.existsByIdAndOwnerAccountId`.
A cross-tenant row never leaves the database, so a future query built on these methods cannot leak
it by omission the way a fetch-then-check in Java could. (A full schema-per-tenant design exists in
`~/Documents/MULTITENANCY_SYSTEM_DESIGN.docx`, but it describes an unrelated project — CIC
InsureMaster, a bancassurance platform whose tenants are separate banks and insurers needing full
data-plane isolation. Meridian's vendors are rows in one schema, not organisational tenants; that
document's schema-per-tenant machinery does not apply here.)

### Catalog moderation (`catalog`)

A product is now created **DRAFT**, not immediately ACTIVE (`ProductStatus`'s javadoc records which
of BR-CAT-06's six states this phase does and does not implement — no Pending Review, Out of Stock
or Rejected). `POST /catalog/products/mine/{id}/publish` moves DRAFT → ACTIVE, refusing when no
image exists (BR-CAT-05, AC-CAT-02) or the title matches `prohibited_term` (BR-CAT-08, AC-CAT-03 —
V8 seeds placeholder terms; OPEN-07's real list is still not drafted). `PUT
.../mine/{id}/price` records every change in `product_price_change`, retrievable via
`.../price-history` (BR-CAT-09, AC-CAT-04). A public `GET` 404s a DRAFT or DELISTED product exactly
like a nonexistent one.

### `shared.BlobStorage`

Vendor verification documents and product images both needed "accept bytes, hand back a reference,
retrieve later" — one abstraction, `LocalFilesystemBlobStorage` as the default adapter, matching
ADR-006's reasoning even though nothing external is being called yet: INT-06 is Critical and has no
real provider, and this is what lets upload be built and tested before one exists.

### Known gaps carried forward

- **Idempotency-key handling.** The frontend sends one on vendor application submission (and would
  on order creation); nothing honours it yet. A cross-cutting concern for `web`, not built once per
  module — see `VendorApplicationController`'s class doc.
- **AC-CAT-01's "logs the attempt"** is an SLF4J warning, not a persisted, queryable event. The
  isolation itself does not depend on the log (see the WHERE-clause note above), but an admin
  wanting a history of cross-vendor probing attempts has nothing to query today.
- **No reactivation path** for a suspended vendor. BR-VND describes none; building one was not
  assumed.
- **`admin` module aggregation** does not exist; the approval queue and suspend endpoints live in
  `vendor.internal`, method-secured per endpoint.

## Phase 2/4: inventory and checkout

BR-INV-01 to 08 (AC-INV-01 to 04) and BR-CHK-01/02/04/09 are now built. Per-product stock, not
per-variant — the same reduction as catalog's deferred BR-CAT-04.

**Two counters, not one** (`stock.total_quantity`, `stock.reserved_quantity`; available is always
computed as their difference, never stored). BR-INV-02/03/05/06 describe three different things
happening to the same number, and AC-INV-03 ("total stock reduced exactly once") only holds if
reserving and confirming touch different counters — see V9's migration comment.

**Concurrency (AC-INV-01) is a single atomic UPDATE with the availability check in its own WHERE
clause**, not a read in Java followed by a write — `StockRepository.tryReserve`. Verified with a
real test: eight threads parked on a latch, released simultaneously against one unit of stock;
exactly one succeeds (`InventoryConcurrencyTests`). This is the one place in the codebase where a
naive read-then-check-then-write would have been a genuine, hard-to-reproduce production bug.

**Reservation-to-deduction (BR-INV-05) is event-driven, not a direct call.** `order` publishes
`OrderPaymentConfirmed` (already existed from Phase 1); `inventory`'s own listener consumes it and
converts every reservation referencing that order. `order` gained no dependency on `inventory` to
make this work — ADR-004's pattern, again.

**Expiry (BR-INV-03, AC-INV-02) is a `@Scheduled` sweep**, not a lazy check on read — "stock
returns to available after 15 minutes" describes something that has to happen without anyone
asking. Runs every 30 seconds; each reservation resolves in its own transaction so one bad row
can't block the rest.

### Checkout (`checkout`) retires Phase 1's direct order creation

`POST /orders` (Phase 1's single-product walking-skeleton stand-in) is gone. `order.create` was
extended to accept multiple lines and does its own BR-ORD-02 vendor-grouping into sub-orders;
`checkout.CheckoutService` is the sole caller, reached via a new public `order.OrderCreation`
interface — **the first genuinely synchronous cross-module write call in this codebase**, as
opposed to every other write, which happens inside its own module or is announced by an event.
The reason is atomicity: checkout needs order creation and stock reservation to succeed or fail
together in one transaction, so a buyer never receives an order with no stock behind it. An event
cannot give that guarantee — its own transaction commits separately (ADR-004) — so this one
crosses the boundary directly instead, documented as the deliberate exception it is in
`OrderCreation`'s own javadoc.

BR-CHK-09 (no checkout against a suspended vendor) is checked **twice, for different reasons**:
once implicitly, because a suspended vendor's products get delisted (Phase 2's
`VendorSuspensionListener`) and delisted products fail `order.create`'s own availability check;
and once explicitly, in `CheckoutService`, against live vendor status — because that listener is
`@Async` (confirmed from its own annotation source in Phase 2), so there is a real window between
suspension and delisting taking effect that the first check alone does not close.

**Cart persistence (BR-CHK-03) was not built** — downgraded to Should in v2.2's MoSCoW
re-baseline, and `checkout` deliberately holds no state: the caller submits cart contents on every
call. **Delivery cost (BR-CHK-05/06/07) was not built** — `fulfilment` (Phase 7) exists now (see
below), but `checkout` is not wired to it: no buyer-facing delivery-method selection, no rate-card
lookup, no delivery line added to the order total. `ParentOrder.total` stays item total only. This
is genuinely separate follow-up work now, not blocked on a module that does not exist.

### A real infrastructure fix, not a workaround: shared Testcontainers Postgres

`./gradlew build` started failing intermittently once enough `@SpringBootTest` classes existed —
each one starting its own PostgreSQL container, several alive at once on a memory-constrained
machine. Confirmed as resource contention, not a code defect, by running the failing classes in
isolation (always passed) and checking system memory during a full run (11GB/15GB used, swap
active). `TestcontainersConfiguration` now starts one container in a static initializer, shared by
every Spring context in the JVM, rather than one per context. Verified across two consecutive full
builds under the same memory pressure — reliably green, not a lucky retry.

## Phase 0/3/6: pricing, payment, ledger, settlement

BR-PAY (minus refunds), BR-LDG-01 to 06, and a reduced BR-STL-01/02/03/06 to 12 are now built,
plus COM-01 to 05's commission versioning. Together these are the money path: a payment
confirmation posts a balanced ledger entry, and a settlement run disburses what the ledger says a
vendor is owed.

**Commission is versioned, resolved once, never re-resolved (COM-05).** `pricing` owns
`commission_configuration` (rate + `effectiveFrom`), seeded at 9%. `order.create` resolves
`pricing.currentCommissionRate()` once per order — not once per vendor group, because every
sub-order in one checkout prices under the same version — and stores the
`commissionConfigurationId` on each sub-order. `order.markPaymentConfirmed` resolves the rate again
later through `pricing.rateForVersion(id)`, against that stored id, never against "the current
rate": a rate change after an order is placed cannot change what that order settles at.

**`payment` depends on nothing**, matching ADR-002's "money flows one way" rule. It cannot look an
order up to find its amount, so `checkout` supplies `Money amount` directly to
`PaymentInitiation.initiate(parentOrderId, amount, buyerAccountId, buyerPhoneNumber)` — the
amount travels with the call instead of being re-derived. `PaymentTransactionRepository.tryConfirm`
/`tryFail` are the same atomic-UPDATE-with-the-precondition-in-its-WHERE-clause pattern as
`inventory.StockRepository.tryReserve` (BR-PAY-05): a replayed provider callback resolves to "already
handled", not a second `PaymentConfirmed`. `MockPaymentGateway` stands in for the real aggregator
(OPEN-03) and always succeeds instantly.

**`ledger` posts one balanced set of entries per sub-order, validated in Java before any row is
written.** Since `ledger`'s allowed dependencies are event-only (`payment::events`, `order::events`,
`returns::events`), `order.events.OrderPaymentConfirmed` carries a fully resolved
`List<SubOrderSettlementDetail>` — subtotal, commission rate, commission configuration id per
sub-order — so `ledger.LedgerPostingListener` never needs a direct dependency to compute what it
posts. Four accounts for this pass (`COLLECTIONS_CLEARING`, `COMMISSION_REVENUE`, `VENDOR_PAYABLE`,
`HOLDBACK_RESERVE`); delivery cost and refunds are not modelled, because `checkout`'s delivery
pricing and `returns` (Phase 5) do not exist yet — see `LedgerAccount`'s javadoc. Holdback is a flat
10% (`RoundingMode.DOWN`) taken off the vendor's portion, an indicative figure from BRD 5.9 (OPEN-05,
not yet a configured value) — the same "indicative, not configured" treatment
`PayoutDetailEntity.COOLING_OFF` already gives its own BRD-sourced figure.

**`order.OrderCreation` and `ledger.LedgerPosting` are this codebase's other two documented
synchronous cross-module calls**, alongside `checkout`→`order`. `LedgerPosting.postDisbursement`/
`postHoldbackRelease` are direct calls because `settlement` must know a posting succeeded before
treating a disbursement as having happened — see that interface's own javadoc.

### `settlement`, deliberately reduced

BR-STL-04 (withhold orders inside the return window) and BR-STL-05 (release holdback when that
window closes) are **not implemented**. Both need `returns` (Phase 5), which does not exist — the
BRD's own v2.2 roadmap (§18, "why returns precede settlement") argues this sequence isn't fully
exercisable without it. This takes the same explicit, documented reduction as every other
cross-phase gap in this codebase rather than blocking: every sub-order's holdback sits in
`HOLDBACK_RESERVE` indefinitely, with no automatic release path, until `returns` lands. Matching
that, `settlement`'s actual `allowedDependencies` is `{ledger, vendor}`, not the `{ledger, vendor,
order, returns, pricing}` ADR-002's table lists — `settlement/package-info.java` explains why
`order`, `pricing` and `returns` aren't needed yet and what would bring each back.

**Everything else is real.** `initiate(currency, actor)` first checks `ledger.TrialBalance
.isReconciled()` (BR-STL-10; a new ledger capability — sums every entry ever posted to zero, which
holds by construction as long as nothing bypassed `LedgerService.post`'s own balance check, so this
is a defence against a write that went around the application entirely, not a redundant check) —
an unreconciled ledger produces a `BLOCKED` run with no lines at all (AC-STL-02). Otherwise it walks
`ledger.VendorLedger.vendorsWithPayableBalance(currency)` (a new ledger capability, added alongside
`TrialBalance` for the same reason) and creates one `SettlementLine` per vendor: `EXCLUDED` with a
reason (`NO_PAYOUT_DETAILS`, `PAYOUT_DETAILS_COOLING_OFF`, `MANUAL_HOLD`) or `PENDING`, using
`vendor.Vendors.payoutCoolingOffUntil` — built in Phase 2 specifically for this (BR-STL-08).
`approve(runId, actor)` (BR-STL-11) is the only path to `APPROVED`; the controller then calls
`disburseLine` for each `PENDING` line.

**BR-STL-12's idempotency key is generated once, at line creation, and never changes** — every
disbursement attempt for a line, including every retry, sends the same key, matching what "at most
one disbursement regardless of retry" actually requires. `SettlementLineRepository.tryStartDisbursing`
is the same atomic-claim pattern as payment's `tryConfirm`. A `MockDisbursementGateway` mirrors
`MockPaymentGateway` and always succeeds, so `SettlementRetrySweeper` (BR-STL-07, structured exactly
like `inventory.ReservationExpirySweeper`) has no failure to actually retry yet — the mechanism is
built, not exercised, until a real provider or a deliberately-failing test double exists.

**The vendor-facing balance endpoint lives in `settlement`, not `ledger`.** It was first written on
`ledger.internal.LedgerController`, importing `vendor.Vendors` to check ownership — a real
`ModularityTests` violation caught by the full build (`ledger`'s allowed dependencies are
event-only), not by `compileJava`. Rather than adding `vendor` to `ledger`'s dependencies, the
endpoint moved to `settlement`, which ADR-002 already names as allowed to depend on both — the
correct home for it architecturally, not a workaround: a vendor's disbursable balance is a
settlement concern before it is a raw ledger one.

**`DisbursementGateway` never receives a vendor's actual payout destination.** That value is
sensitive-adjacent inside `vendor` itself — `PayoutDetailService`'s own read model never returns it,
even to `vendor`'s own callers. Exposing it across the module boundary for disbursement would cross
that deliberate line; the mock resolves nothing and disburses "to vendorId", which is enough to
exercise BR-STL-06/07/12 without punching a hole through it now. A real integration needs `vendor`
to expose a narrowly-scoped resolution capability for disbursement specifically — recorded future
work, not an oversight.

### A real bug, not a typo: `Optional.map` collapsing a null result

`SettlementService.exclusionReasonFor` first tried
`vendors.payoutCoolingOffUntil(vendorId).map(t -> now.isBefore(t) ? "REASON" : null).orElse("NO_PAYOUT_DETAILS")`
— intending the mapper's `null` to mean "no exclusion". `Optional.map` treats a null-returning
mapper as producing an empty `Optional` (its own documented behaviour), so `.orElse(...)` fired for
*both* "no payout details" and "cooling off already passed", making every vendor with details on
file look excluded regardless of how long ago they were set. Caught by
`settlementRunDisbursesEligibleVendorAndExcludesIneligibleOne`, not by inspection — the fix is
imperative `Optional` inspection (`isEmpty()` / `.get()`) instead of chaining through `map`.

## Phase 5: returns, refunds and disputes

BR-RTN-01 to 08 are built; BR-RTN-09 (return rate tracking, Should) is not — see `V17__returns.sql`'s
header comment. This lands one phase ahead of the BRD's own v2.2-recommended sequencing (§18: "why
returns precede settlement" argues returns should precede settlement, which it now does — `returns`
was built after `settlement`, in response to "continue with returns" rather than a strict replay of
the roadmap's phase order). Everything BR-STL-04/05 needed from `returns` remains exactly as absent
as `settlement`'s own package-info already documented; nothing here retroactively closes that gap.

**A real prerequisite gap, closed minimally: `OrderStatus.DELIVERED`.** BR-RTN-01 measures a return
window from delivery, and nothing produced a delivered marker — BR-ORD-03's full lifecycle (Accepted,
Dispatched, In Transit, Delivered) needs vendor sub-order acceptance and Phase 7's carrier tracking,
neither of which exists. Rather than build either, `order` gained the one state genuinely needed:
`OrderService.markDelivered`, a vendor-invoked stand-in for Phase 7's real fulfilment trigger,
matching Phase 1's manual payment-confirmation precedent exactly. It is not a retirement candidate —
there is no real trigger yet for it to be retired in favour of.

**BR-ORD-13 to 16 (buyer-initiated cancellation) were deliberately not built in this pass.** They are
filed under BR-ORD, not BR-RTN, and the BRD's own text ties their refund/commission-reversal
consequences to BR-RTN-05's machinery — so they share real infrastructure with returns but need their
own design decision this pass didn't make: inventory's reservation-vs-already-deducted-stock
distinction (RELEASE for a still-PENDING_PAYMENT cancellation vs a genuine RESTOCK for a CONFIRMED
one), and an event payload that lets `inventory` restock only the cancelled sub-order's lines out of
a multi-vendor parent order. A real, separate piece of work — not an oversight folded silently in.

**`returns`'s actual `allowedDependencies` is `{order, payment, vendor}`, not ADR-002's `{order,
payment}`.** `vendor` is a deliberate addition: BR-RTN-03's vendor accept/reject action needs to
verify the caller owns the vendor on the return, and routing that through `order` would misplace
vendor-ownership authorization in a module that does not own it — the same anti-pattern the
settlement/ledger arc's fix corrected, applied here before it became a `ModularityTests` failure
instead of after. `pricing` is absent: `order.SubOrderDetail` — a new cross-module read type,
alongside `order.Orders.findSubOrder` — already carries the resolved commission rate (`order`
resolved it via `pricing.rateForVersion` for its own use), so `returns` never needs `pricing` itself,
matching `order.events.OrderPaymentConfirmed.SubOrderSettlementDetail`'s reasoning one module over.
`SubOrderDetail` also carries `lines: List<OrderLine>` — BR-RTN-04's partial refund is computed from
the sub-order's actual purchased lines and quantities against what the buyer requests back, never
taken as a buyer-supplied figure, which would be a straightforward abuse vector (RSK-08).

**The refund itself lives in `payment` (`Refund`, `RefundInitiation`, BR-PAY-11), not `returns`** —
matching the BRD's own 15.1 module-ownership table. `returns` calls `payment.RefundInitiation
.initiate(parentOrderId, amount, reason)` synchronously — a return must know the refund succeeded
before considering itself resolved, the same "these two things succeed or fail together" reasoning
as `order.OrderCreation` and `ledger.LedgerPosting`. `MockRefundGateway` mirrors `MockPaymentGateway`
and `settlement`'s `MockDisbursementGateway` exactly: always succeeds instantly. `Refund` is its own
append-only row against `payment_transaction`, not a status flag on it — BR-RTN-04 allows several
partial refunds against one transaction, and `PaymentStatus.REVERSED` stays unused, reserved for a
transaction refunded in full, which nothing tracks yet.

**Commission reversal (BR-RTN-05) is `ledger` listening to `returns.events.ReturnRefunded`**, not a
direct call — `ledger`'s allowed dependencies are event-only, and `ledger` and `notification` had
already declared `returns::events` in anticipation of this module, the same "declare ahead of the
consumer" pattern `vendor::events` and `settlement::events` were built under.
`LedgerService.postRefund` is the exact contra of `postPaymentConfirmed`: same rate, same holdback
split, signs flipped, debiting HOLDBACK_RESERVE before VENDOR_PAYABLE. That ordering is what makes
BR-RTN-08 ("recover refunded amounts from future vendor settlements where holdback is insufficient")
hold with no separate recovery mechanism: if a refund's holdback portion exceeds what was reserved,
HOLDBACK_RESERVE simply goes negative, and `VendorLedger.vendorsWithPayableBalance`'s existing
`> 0` filter already excludes that vendor from any future settlement run, for free.

**BR-RTN-03's timeout escalation** is `ReturnTimeoutSweeper`, structured exactly like
`inventory.ReservationExpirySweeper` and `settlement.SettlementRetrySweeper` — a separate `@Component`
looping over ids and calling `ReturnService.escalateIfTimedOut` through the injected (proxied) bean,
never self-invoked, for the same transaction-boundary reason `SettlementService`'s own class doc
already documents in detail.

**A real bug, not a typo: `String.formatted()` binding to the wrong operand.** A test built its JSON
body as `"A" + "B".formatted(x, y)` across two concatenated string literals — `.formatted` binds to
the immediately preceding literal, not the whole `+`-joined expression, so only `"B"`'s placeholder
was substituted and `"A"`'s literal `%s` reached the server verbatim, failing UUID deserialization.
Caught by the test itself failing with a 500, not by inspection; fixed by parenthesising the full
concatenation before calling `.formatted`. A second, unrelated bug in the same test run — `JsonPath`
returning a `JSONArray` for `path[0]` chained after a `?(filter)` — was the same mistake the
settlement test had already made and fixed once; this pass repeated it before catching it again.

## BR-ORD-13 to 16: buyer-initiated cancellation

Built as a follow-up to `returns`, closing the gap that arc's own doc flagged explicitly. Lives
entirely in `order` for the state transition, fanning out to three modules that already had
standing dependencies to react — no new listener-side dependency was needed anywhere except one.

**`order` gained a real dependency: the full `payment` module, not just `payment::events`.**
`OrderService.cancel` calls `payment.RefundInitiation.initiate(...)` synchronously when the
sub-order being cancelled had already been paid for — the same "these two things succeed or fail
together" reasoning as `checkout`→`order.OrderCreation` and `settlement`→`ledger.LedgerPosting`: a
cancellation must never be recorded without the refund having actually gone out. `payment::events`
stays separately declared alongside it — a full module dependency and its named-interface events
package are distinct grants in this codebase (confirmed by `catalog`'s existing `vendor`/`vendor
::events` split), not one subsuming the other.

**One event, three independent reactions, zero new listener-side dependencies.**
`order.events.SubOrderCancelled` carries everything each consumer needs to act without asking
anything else first:

- `inventory` (already depends on `order::events`) releases a reservation still RESERVED, or
  restocks one already CONFIRMED — a genuinely new capability, `StockRepository.restock`, the exact
  inverse of `confirm`: it gives back `totalQuantity` alone, since `reservedQuantity` was already
  reduced when the reservation was confirmed. A new `ReservationOutcome.CANCELLED` (and matching
  `StockMovementEntity.Reason`) keeps this distinguishable in the audit trail from an ordinary
  pre-confirmation `RELEASED`.
- `ledger` (already depends on `order::events`) reverses commission via the exact same
  `LedgerService.postRefund` built for `returns` — only the `referenceType` string passed to it
  differs (`"SUB_ORDER_CANCELLATION"` vs `"RETURN"`), and only when the event says payment was
  actually confirmed; a sub-order still PENDING_PAYMENT was never charged and has nothing to
  reverse.
- `notification` (already depends on `order::events`) logs the vendor notification BR-ORD-16 asks
  for, with the same DEV-ONLY stand-in and the same documented gap — no vendor contact details
  exist yet — `OrderConfirmationNotificationListener`'s own javadoc already recorded for the
  equivalent buyer-side case.

**BR-ORD-16's other half — excluding a buyer-initiated cancellation from the vendor's cancellation
rate under BR-VND-13 — needed no code at all.** BR-VND-13 (a vendor cancellation-rate metric) is not
built anywhere in `vendor`; there is no rate to wrongly count this against. Recorded as a
pre-existing gap this feature does not introduce, not a requirement satisfied by omission.

**BR-ORD-15** ("show the buyer, for each sub-order, whether cancellation remains available") is a
new `cancellable` field on the public `SubOrder` record, computed identically to the eligibility
check `OrderService.cancel` itself enforces — the same boolean, not two separately-maintained rules
that could drift apart.

## Phase 7: fulfilment and logistics

BR-FUL-01 to 08 are built, minus BR-FUL-05 (auto status update from carrier tracking, Should — no
real carrier webhook exists to drive it, the same "mock always succeeds, nothing exercises the
webhook path" limitation every other mock provider in this codebase already has). `fulfilment`'s
actual `allowedDependencies` is `{vendor, order::events}` — the first module built this session
that matches ADR-002's table exactly, in both directions, with no deviation needed.

**Carrier, DeliveryZone and DeliveryRateCard** (BR-FUL-01/03/08) are real, independently
admin-manageable entities — `DeliveryRateCard` versioned exactly like `pricing`'s commission
configuration (DAT-07: effective-from, never updated in place) — even though nothing charges
against a rate card yet (`checkout` is not wired to `fulfilment`; see that module's own note).
`Carrier` itself has no admin write path in this pass — seeded via migration only — since
BR-FUL-01/03's actual requirement is structural ("carriers can be added without order-processing
changes"), not a promise of a management screen; AC-FUL-03 is satisfied by a second row existing.

**`Delivery` is created automatically, address- and method-less, on `OrderPaymentConfirmed`** — a
walking-skeleton stand-in for the buyer choosing a delivery method at checkout, which does not
exist yet. A vendor books it manually through `fulfilment`'s own controller: `book` (a carrier,
matching BR-FUL-01/03), `dispatch` (a tracking reference, BR-FUL-04) or `dispatchManually` (BR-FUL-06's
actual requirement — "manual status update... where carrier integration is unavailable" — not a
fallback bolted on afterward), `markInTransit` (AC-FUL-02's pickup-point nuance: status stays In
Transit until collection is recorded separately), `markDelivered` (BR-FUL-07's proof of delivery).
`MockCarrierGateway` mirrors every other mock provider in this codebase.

### A real architectural finding, caught by `ModularityTests`, not by inspection: the delivery-completion cycle

The first design tried to have `order`'s own DELIVERED transition driven by `fulfilment`'s
completion — the same "retire the manual stand-in for the real trigger" pattern this codebase
applied to Phase 1's payment confirmation and Phase 5's own `mark-delivered` endpoint at the time
it was written. `order.OrderService.markDelivered` was rewritten to drop its ownership check (no
actor — a trusted internal event, matching `markPaymentConfirmed`'s existing shape), `order` gained
a `fulfilment::events` dependency, and `order.internal.DeliveryCompletionListener` was added to
consume `fulfilment.events.DeliveryCompleted`.

`ModularityTests` refused it: `Cycle detected: Slice fulfilment -> Slice order -> Slice fulfilment`.
ADR-002 already has `fulfilment` depending on `order::events` one-directionally — `fulfilment`
learns a sub-order needs a delivery by listening for `OrderPaymentConfirmed`
(`OrderReadinessListener`) — and adding the reverse edge closed a cycle, event-only or not; Spring
Modulith enforces acyclicity at the module level regardless of whether an edge is a synchronous
call or a listened-for event.

**The fix was to revert, not route around it.** `OrderService.markDelivered` is exactly what it
was before `fulfilment` existed: vendor-invoked, ownership-checked directly against `vendor`.
`fulfilment.events.DeliveryCompleted` is still published (BR-FUL-06/07) but has no consumer —
the same "declare ahead of the consumer" pattern already used for `vendor::events` and
`settlement::events`, just with no consumer arriving yet rather than arriving later. `order`'s
coarse DELIVERED and `fulfilment`'s own, much richer `Delivery.status`
(PENDING/BOOKED/DISPATCHED/IN_TRANSIT/DELIVERED/FAILED) are now two genuinely independent
"delivered" concepts, triggered separately, with no code pretending otherwise. A real fix — should
one be wanted — needs a mediating module (matching `checkout`'s existing dependency on both `order`
and `fulfilment`) rather than a direct edge between the two; that is future work, deliberately not
attempted here, matching ADR-002's own insistence that a dependency-graph change be a considered
decision, not a convenience.

## Phase 3: tax (eTIMS)

REG-04 to 08 are built. `tax`'s actual `allowedDependencies` is `{order::events, returns::events}`
— ADR-002's table lists only `order::events`; `returns::events` is a deliberate addition, matching
the pattern `ledger`, `order` and `returns` themselves already set: REG-08's credit notes are
triggered by two different refunds — `order.events.SubOrderCancelled` (already covered) and
`returns.events.ReturnRefunded` (not, until now) — and both are needed for the one requirement.

**One `TaxInvoice` per parent order, not per sub-order.** 14.1 states Section 10 assumes principal
VAT classification — the platform is the deemed supplier to the buyer, not merely a facilitator —
so it issues one consolidated invoice per order, matching REG-04's own wording ("for every
confirmed order") and UC-11's title ("System Emits a Tax Invoice", singular). Generated and
transmitted off `OrderPaymentConfirmed`, summing every sub-order's subtotal the event carries.

**`CreditNote` lives here, not in `returns`, despite BRD 15.1 filing it under the Returns module's
entity table.** The same reasoning 15.1 itself gives for filing Refund under Payments rather than
Returns applies again: a credit note needs the tax-transmission infrastructure (REG-05/06/07's
retry, dead-letter and acknowledgement handling), not the returns-workflow infrastructure, and it
has to reference the invoice it corrects. Two listeners generate one — `CancellationCreditNoteListener`
(only when `SubOrderCancelled.paymentWasConfirmed`) and `ReturnCreditNoteListener` — both calling
the same `TaxService.generateCreditNote`, which throws `IllegalStateException` if no invoice exists
yet for the order: by the time either refund path can fire, payment must already have been
confirmed, so a missing invoice is a genuine invariant violation, not a race to tolerate.

**REG-05's retry uses a simpler mechanism than `payment`/`settlement`'s atomic-claim pattern,
deliberately.** `tryConfirm`, `tryStartDisbursing` and their kin exist because a duplicate
disbursement or payment confirmation is a duplicate financial movement — BR-STL-12 says so
explicitly. A duplicate eTIMS transmission is not that kind of harm, and REG-04 to 08 do not ask
for exactly-once submission, so `TaxService` uses a plain read-check-write inside one
`@Transactional` method instead. `MockDigiTaxGateway` — named for DigiTax, the intended eTIMS
integration facilitator (DEP-04), not a hypothetical unnamed aggregator — always succeeds, so
REG-05's retry path and REG-06's dead-letter queue are both built but, like every other mock
provider's failure path in this codebase, unexercised by anything but a deliberately-failing test
double.

**REG-06's "admin exception queue" and REG-07's "retain... invoices and acknowledgement
references" are two different read paths on the same data** — `AdminTaxController` exposes both:
`/admin/tax/invoices/exceptions` (FAILED and DEAD_LETTERED only) and `/admin/tax/orders/{id}/invoice`
(REG-07's retention — including the ordinary TRANSMITTED case, which the exception queue alone
cannot show).

## Phase 8: reviews and ratings

BR-REV-01 to 06 are built, all six of them Should — but the BRD's own 10.12 is explicit that "if
reviews ship, BR-REV-01, 05 and 06 are not optional within them: an unverified review system is
worse than none." This module is built to that standard, not a reduced one, even though the module
as a whole stays a Should.

**`vendor` is a deliberate addition beyond ADR-002's table**, which lists only `order` and
`catalog` for `review` — matching the pattern `settlement`, `returns`, `order` and `tax` themselves
already set. BR-REV-06 ("prevent a vendor from reviewing their own products") and BR-REV-04 (a
vendor responding to a review on their own product) both need to resolve "does this account own
the vendor behind this product", which only `vendor` can answer.

**BR-REV-01's "a delivered order for that product" is a new, narrowly-scoped capability on
`order`**: `Orders.hasDeliveredProduct(buyerAccountId, productId)`, backed by a single repository
query joining sub-order, its lines and its parent order's buyer — the exact question `review`
needs answered, nothing more (not, say, a general "list this buyer's delivered products" that
would let `review` iterate a buyer's whole purchase history). BR-REV-06 is checked *before*
BR-REV-01 in `ReviewService.submit` — a vendor reviewing their own product is refused on that
basis regardless of whether they also happen to have a delivered order for it.

**One review per buyer per product** is enforced at the database level (`review_buyer_product_unique`)
— not a BRD requirement verbatim, but the obvious reading of "a numeric rating" from "buyers with a
delivered order": one considered opinion per purchase relationship, not an unbounded number of
re-reviews.

**Reviews are nested under `/api/catalog/products/{id}/reviews` in the URL surface**, even though
the code implementing them lives in `review`, not `catalog` — a deliberate REST design choice
(reviews are conceptually a sub-resource of the product) that costs nothing architecturally, since
`@RequestMapping` paths are independent of module/package structure. The upside: `GET
/api/catalog/products/**`'s existing `permitAll()` rule (BR-IAM-08's guest browsing) already covers
the public listing and summary endpoints with no `SecurityConfiguration` change needed — only the
vendor storefront summary (`/api/vendors/{id}/reviews/summary`, BR-VND-11) needed a new rule, since
it falls outside that path.

**BR-REV-03's aggregation, and BR-VND-11/BR-SCH-02's dependence on it, stay real but unconsumed
cross-module** — `review`'s own `RatingSummary` endpoints are genuine, queryable now; nothing in
`vendor`'s storefront view or a `search` module (which does not exist) calls into them
programmatically yet. The same "build the contract, wire the consumer later" pattern this
codebase has used throughout, most recently for `fulfilment.events.DeliveryCompleted`.

## Phase 8: search and discovery

BR-SCH-01 to 06 are built, with three pieces of BR-SCH-02/04 deliberately deferred. `search`'s
actual `allowedDependencies` is `{catalog}` — matching ADR-002's table exactly, the second module
this codebase has built with no deviation either way (the first was `fulfilment`). That match is a
deliberate scope choice, not an accident: BR-SCH-02's rating and delivery-availability filters need
`review` and `fulfilment`; BR-SCH-04's out-of-stock exclusion needs `inventory`. Rather than add
those dependencies opportunistically because the data happens to exist elsewhere now, this pass
keeps `search`'s dependency exactly as ADR-002 states and documents the three gaps explicitly —
category filtering is not merely deferred but genuinely blocked, since BR-CAT-03 (category
taxonomy) does not exist anywhere in `catalog` for anything to filter by.

**`catalog.ProductSearch` is a new public query primitive**, not a `search`-owned index — BR-SCH-01's
keyword matching, BR-SCH-02's price/vendor filters, BR-SCH-03's price/recency/relevance sorts, and
BR-SCH-04's active-only default (which for free also satisfies "exclude delisted and
suspended-vendor products" — a suspended vendor's products are already DELISTED by
`VendorSuspensionListener` before this is ever queried) all resolve there. `search`'s own value is
BR-SCH-06's zero-result logging and being the one place a *search experience* lives, separate from
`catalog`'s own data ownership — adding a rating filter once `review` is wired in, say, is a change
to `search`, not to `catalog`'s query surface.

**BR-SCH-03's relevance sort is a genuine, if basic, signal — not recency renamed.** A dedicated
query (`ProductSearch.searchByRelevance`) ranks a name match above a description-only match, ties
broken by recency; it requires an unsorted `Pageable`, since the query's own `order by` is the
ranking and Spring Data would append a caller-supplied sort after it rather than replace it.
BR-SCH-05's "partial matches" is real case-insensitive substring matching (`ILIKE`); true
misspelling tolerance (Levenshtein or trigram-based correction) is not built.

### A real bug, not a typo: Postgres inferring `bytea` for a null keyword parameter

`ProductRepository.search`'s original query used `(:keyword is null or lower(p.name) like
lower(concat('%', :keyword, '%')) ...)`. Every keyword-less search — a plain filtered browse —
failed with `function lower(bytea) does not exist`: Postgres could not infer `:keyword`'s type from
a bare `lower(?)` when the bound value was null, and defaulted to `bytea`. Caught by
`searchFindsFiltersSortsAndLogsZeroResults`'s own price-range assertion (no keyword supplied),
not by inspection — fixed by wrapping the parameter in `cast(:keyword as string)`, a no-op for a
non-null value that gives Postgres an explicit type either way.

### A second bug in the same test: an unscoped keyword and price filter colliding with other tests' data

The first version of the test searched for the literal keyword `"widget"` and filtered on
`minPriceMinorUnits=150000` with no vendor scope — both matched products created by *other* test
methods in the same shared, non-rolled-back database (several of which also name their products
"...Widget..." or price above that threshold), not just the two this test created. `hasSize(1)`
failed with 5. This is the same test-execution-order fragility this codebase has hit and fixed
before (`awaitDelisted`'s own comment records the general rule); the fix here was a nonsense,
per-run-unique keyword prefix and scoping the price-range check to the test's own `vendorId`.

## Phase 9: notifications (BR-NTF)

Phase 1/2's four listeners were DEV-ONLY log lines — no gateway, no retry, no way to address a
real phone number or email. This phase replaces them with a real outbox (`notification_message`,
BR-NTF-05's retry/dead-letter lifecycle mirroring `tax.TaxDocumentStatus` exactly), a mock SMS and
a mock email gateway behind `SmsGateway`/`EmailGateway` (ADR-006), and a retry sweeper mirroring
`tax.internal.TaxRetrySweeper`.

**`notification`'s `allowedDependencies` is a deliberate superset of ADR-002's table**: it adds
`iam` and `vendor` (the base modules, not just their `::events` interfaces already listed) to what
ADR-002 states. Every event this module consumes — barring `OneTimeCodeIssued`, which already
carries its own resolved destination — names an account or vendor by id, never by phone number or
email. An event-only dependency can log that id; it cannot address a gateway. Real delivery needs
`iam.Accounts` to resolve an account id to contact details, and `vendor.Vendors` to resolve a
vendor id to its `ownerAccountId` first. Both are read-only query facades, the same shape `returns`
and `review` already took `vendor` for; neither introduces a cycle, since `iam` depends on nothing
and `vendor` depends only on `iam`. `notification.internal.ContactResolver` is the one class that
uses either — every other class in this module works in phone numbers, emails and message bodies.

**BR-NTF-01's dispatch and delivery SMS are not sent — recorded as a genuine gap, not an oversight.**
`fulfilment.events.DeliveryCompleted` carries a `subOrderId`, not a `buyerAccountId`; resolving who
to notify would need `order` on top of `fulfilment::events`, two more dependencies added
opportunistically rather than because a concrete gap forced them the way `iam`/`vendor` did above.
Order-confirmation SMS is real; dispatch/delivery notification is left for a pass that takes on
that scope deliberately — the same kind of honestly-recorded deferral as `search`'s category filter.

**BR-NTF-02's "new orders" and "settlement completion" are both real sends**, not just the buyer
half of BR-NTF-01: `OrderConfirmationNotificationListener` notifies every vendor with a sub-order in
a confirmed `OrderPaymentConfirmed`, and a new `SettlementNotificationListener` notifies a vendor on
`VendorDisbursed`. A third, unenumerated-but-natural extension — `ReturnRefundNotificationListener`
on `ReturnRefunded` — tells a vendor when a refund is issued against their sub-order, using an event
ADR-002 already listed among this module's dependencies without anything consuming it yet.

**BR-NTF-04's opt-out is real infrastructure with no caller yet.** `notification_preference` (keyed
by account id, absence meaning "not opted out") and `NotificationService.sendMarketing` gate on it;
`NotificationPreferenceController` lets a buyer read and set their own preference. Nothing in this
codebase generates a marketing send yet — there is no marketing-campaign feature anywhere in the
built modules — so this is infrastructure ahead of use, not a requirement satisfied by omission.
Every listener in this module calls `sendTransactional`, which the opt-out never gates, matching
BR-NTF-04's "retains transactional notifications" exactly.

**The mock gateways are deliberately unbranded**, unlike `tax.internal.MockDigiTaxGateway`: no
specific SMS or email provider has been named for this integration (a Kenyan bulk-SMS aggregator
such as Africa's Talking is the likely real replacement), and naming one without an explicit steer
would be a guess this codebase's other mocks have avoided making.

Verification: `WalkingSkeletonFlowTests.ordersAndApplicationsDriveRealNotifications` exercises the
whole chain over real HTTP — vendor-application approval, order confirmation, and buyer
cancellation each produce a real, retrievable SENT message via the new `/admin/notifications`
exception-and-history endpoint, plus the self-service opt-out round-trip. Full `./gradlew build`
checkpoint: clean, no infra crash, every one of the 9 test-result XML files fresh with
`failures="0" errors="0"` (41 tests total), checked directly rather than trusted from the top-level
marker.

## Phase 9: platform integrity and abuse controls (BR-SEC)

BR-SEC-01/02/06/07/08 (all Must priority) are built and real. BR-SEC-03/04/05 (all Should) were
deliberately deferred at this phase; they were closed out in a later pass — see
["BR-SEC-03/04/05: order-origin search and cross-account abuse signals (closing a fourth deferred
gap)"](#br-sec-030405-order-origin-search-and-cross-account-abuse-signals-closing-a-fourth-deferred-gap)
below, and `integrity/package-info.java` for the current, real state of each.

**`web.RateLimiter` is the real mechanism, and `integrity` does not own the endpoints it protects.**
`iam`, `checkout` and `search` each call it directly — OTC issuance/verification, checkout
initiation, search — without gaining a dependency on `integrity`, because the interface lives in
`web`, the OPEN foundational module every module can already use. This is the same "define the slot
where it's used, fill it from where the feature lives" pattern `sessionAuthenticationFilter`
established for authentication, applied to rate limiting. `integrity`'s own `allowedDependencies` is
empty — a subset of ADR-002's `iam` entry, since nothing built this phase needs to resolve an id
back to an account.

**Identifier-scoped and source-address-scoped rules are separately configured, not one number split
two ways.** BR-SEC-01's "by account and by source address" sounds like one rule checked against two
keys; it can't be, because a source address behind Kenyan mobile carrier CGNAT — or a test suite
running every request through one MockMvc instance — legitimately represents thousands of distinct
callers. A single threshold tight enough to catch one abusive phone number or account would lock out
everyone else sharing that address. `web.RateLimiter` therefore exposes paired `*_IDENTIFIER`/
`*_ACCOUNT` and `*_SOURCE` scope constants per protected endpoint, each with its own configurable
`RateLimitProperties.Rule` — source-scoped defaults are deliberately far more permissive.

### A real bug, not a design choice from the start: breach bookkeeping erased by its own rollback

The first version of `RateLimiterService` recorded a breach's audit event and any resulting
`Restriction` in the same transaction as the `tryConsume` call that found the limit exceeded. Every
caller reaching a breach does so on its way to throwing `ApiException(RATE_LIMITED)`, which rolls
that whole transaction back — erasing the very audit row the breach was supposed to leave behind,
every time, since the breach and the exception are the same event. A test driving three real
breaches for one phone number and expecting an escalated, admin-visible restriction (BR-SEC-07/08)
failed with an empty restriction list — that's what surfaced this. Fixed by moving the breach
bookkeeping into `RateLimitBreachRecorder`, a separate bean whose `REQUIRES_NEW` transaction commits
independently of the caller's rollback — the same fix, for the same reason, as
`inventory.internal.OversellAttemptRecorder` (below). The rate-limit bucket's own increment needed
no such fix: a refused request's increment rolling back is harmless, since the count that matters is
whichever value last committed, and that only changes on an *allowed* request, which always commits.

### BR-SEC-06 lives in `inventory`, not `integrity`

`inventory.internal.OversellAttemptEntity` records the signal at the exact point BR-INV-04's
database constraint (`stock_reserved_within_total`) already prevents true overselling from ever
landing — the reservation attempt that would have exceeded available stock. Recording it required
the same rollback-survival fix as the breach bookkeeping above: `reserve()` throws
`InsufficientStockException` immediately after, which — as the pre-existing
`checkoutRefusesInsufficientStockAndCreatesNoOrder` test already established — rolls back the whole
checkout transaction. `OversellAttemptRecorder`'s `REQUIRES_NEW` is what lets the near-miss survive
that rollback. `AdminInventoryController` exposes it, matching every other admin-ahead-of-`admin`
surface this codebase has built.

Verification: `WalkingSkeletonFlowTests.repeatedOtcRequestsAreRateLimitedAndEscalateToARestriction`
drives real breaches through `/api/auth/code` to a real escalation, then lifts the resulting
restriction as an admin and confirms the reversal (AC-SEC-02); `checkoutRefusesInsufficientStockAndCreatesNoOrder`
was extended to assert the oversell attempt itself is now real and admin-visible. Full `./gradlew
build` checkpoint: clean, no infra crash, every one of the 9 test-result XML files fresh with
`failures="0" errors="0"` (42 tests total), checked directly rather than trusted from the marker.

## Phase 9: administration and back-office (BR-ADM) — the last module

`admin`'s `allowedDependencies` is `{order, payment, pricing}` — a real subset of ADR-002's fuller
`vendor, catalog, order, payment, ledger, settlement, returns, pricing, integrity`. That fuller list
was the target for a module that owns every admin surface; what actually happened, this session, is
that every module built an admin need of its own before this one existed and put it in its own
`internal` package the moment BR-ADM required it — `vendor`'s approval queue, `returns`'s dispute
queue, `fulfilment`'s zone/rate-card configuration, and, this same phase, `catalog`'s moderation
controller and `payment`'s exception queue. Duplicating any of those here would be two paths to the
same state, one of them stale the instant the other changes. `admin` depends only on what it
actually calls: `order` and `payment` for BR-ADM-02's cross-vendor order search, `pricing` for
BR-ADM-06's commission-rate piece.

**BR-ADM-07's audit log is the one piece of state this module genuinely owns**, and it follows the
same slot pattern `RateLimiter` established in Phase 9's `integrity` work: `web.AuditLog` is an
interface in the OPEN foundational module, real-implemented by `admin.internal.AuditLogService`.
Any module can write to it without a dependency on `admin` — wired this phase into vendor
application approval/rejection (previously audited only by a domain event, not by anything an admin
could review "who did this and why" against), catalog moderation, and commission configuration
changes. Every other admin action already built ahead of this module — suspension, dispute
resolution, review removal, tax exceptions, restriction lifts — keeps its own existing local record
and is not yet wired to the shared log; real, scoped-down follow-up work, not a silent gap.

**BR-ADM-02's order search is real across three of the four named dimensions.** `order.Orders`
gained `findByBuyerAccountId`/`findByReference`; a new `payment.Payments` interface (matching
`iam.Accounts`'s read-only-by-design reasoning) resolves a payment reference back to its order.
"By phone" is not built: `order` never persisted the buyer's phone number on a `ParentOrder` — only
`payment` and `notification` ever received it — and resolving one to an account would need `iam`,
which nothing here has a concrete reason to depend on yet.

**BR-ADM-03 makes `PaymentStatus.TIMED_OUT` real** — the enum constant existed since Phase 3 with
nothing ever setting it. A new `PaymentTimeoutSweeper` (mirroring `tax.internal.TaxRetrySweeper`)
transitions a stalled `AWAITING_CONFIRMATION` transaction after 30 minutes, atomically, the same
precondition-guarded `UPDATE` pattern as `tryConfirm`; `payment.internal.AdminPaymentController`
lists FAILED, TIMED_OUT and still-AWAITING_CONFIRMATION ("unreconciled") transactions together.

**BR-ADM-06 was commission rate only, at this phase.** Delivery zones are already real in
`fulfilment`, not duplicated. Holdback percentage was a bare, unversioned `BigDecimal("0.10")`
inside `ledger.internal.LedgerService`, and settlement cycle did not exist as a concept — every run
was admin-triggered on demand. Both were closed out in a later pass — see ["BR-ADM-06: holdback
rate and settlement cycle configuration (closing a fifth deferred
gap)"](#br-adm-06-holdback-rate-and-settlement-cycle-configuration-closing-a-fifth-deferred-gap)
below. Commission rate itself had a real, versioned data model since Phase 0 (COM-05, DAT-07) but no
way to create a new version at runtime — `V10` seeded the only row that ever existed.
`pricing.PricingAdministration` (deliberately separate from the read-only
`pricing.PricingConfiguration`) and `AdminConfigController` close that gap for real.

### A real bug this test caught: a commission-rate test polluting every other test's math

The first version of the new admin test created a commission configuration with `effectiveFrom`
defaulted to "now", which immediately became the live current rate — in the same shared,
non-rolled-back Testcontainers database every other test in the suite runs against. Two unrelated,
already-passing tests computing expected commission and refund amounts against V10's seeded 9%
default failed immediately after, both at the same assertion. Fixed by setting the test's
`effectiveFrom` decades in the future, which exercises the real creation mechanism — validation,
persistence, the audit log entry — without ever letting `PricingConfiguration.currentCommissionRate`
pick the row up, the same "own your test data, don't touch shared state you don't need to" lesson
`search`'s test collision taught earlier this session, this time against *configuration* rather
than product listings.

**BR-ADM-08 (impersonation) was deferred at this phase** — the one Could-priority item in BR-ADM,
and a feature that is actively worse half-built than absent: "logged and visible to the vendor" is
load-bearing, not an add-on, so it earned its own deliberate pass rather than being folded in
alongside everything else this phase. That pass happened later — see ["BR-ADM-08: admin
impersonation of a vendor account (closing the sixth and last deferred
gap)"](#br-adm-08-admin-impersonation-of-a-vendor-account-closing-the-sixth-and-last-deferred-gap)
below.

Verification: `WalkingSkeletonFlowTests.adminToolsAreRealAndAudited` exercises the whole surface
over real HTTP — an audited vendor approval, three independent order-search dimensions each
resolving to the same real order, the payment exception queue, an admin moderation delist (and its
own audit entry), a real versioned commission-rate change, and the role gates on all of it
(`PLATFORM_ADMIN` vs `FINANCE_ADMIN` vs neither). Full `./gradlew build` checkpoint below.

## BR-ORD-05: vendor acceptance window (closing a deferred gap)

`OrderStatus` gains `ACCEPTED` — deliberately a sub-order-only value, never set on a parent order.
BR-ORD-04's "derive parent status from the aggregate of its sub-orders" was never fully
implemented even before this value existed: `cancel` and `markDelivered` already moved only a
sub-order's own status, leaving the parent's frozen at CONFIRMED once payment clears. `accept` and
the new auto-cancellation both follow that same, already-established precedent rather than
introducing a new one — fixing BR-ORD-04's aggregation is real, separate work this pass does not
attempt.

**Two outcomes, one shared mechanism.** `OrderService.accept` (vendor-invoked, ownership-checked
via `findByIdAndVendorId`, the same NOT_FOUND-on-mismatch pattern as everywhere else) and
`OrderService.autoCancelUnaccepted` (system-invoked, no actor) both exist for BR-ORD-05's "within a
configurable window, auto-cancelling if not accepted." The window itself
(`OrderService.ACCEPTANCE_WINDOW`, 48 hours) is a constant, not yet sourced from configuration —
same treatment as `inventory.internal.InventoryService.RESERVATION_TTL`. A new
`AcceptanceWindowSweeper`, structured like `tax.internal.TaxRetrySweeper`, finds CONFIRMED
sub-orders past their deadline and calls through the injected, proxied `OrderService` per id.

**`cancel` was refactored to share its mechanics with auto-cancellation**, not duplicate them: a
new private `cancelInternal(subOrder, actorAccountId, reason)` — `actorAccountId` null for the
system path, the same convention `markPaymentConfirmed` already established for its own
provider-driven transition — backs both `cancel` (BR-ORD-13, buyer-invoked, reason "Buyer
cancellation") and `autoCancelUnaccepted` (BR-ORD-05, reason "Vendor did not accept within the
acceptance window"). `SubOrderCancelled` gained a `reason` field carrying exactly that string, so
`notification.internal.OrderCancellationNotificationListener`'s message to the vendor says which of
the two actually happened rather than assuming it was always the buyer — and, once BR-VND-13 (a
vendor cancellation-rate metric, not yet built) exists, `reason` is exactly what would let it
exclude the buyer-initiated case per BR-ORD-16 while still counting the vendor's own lapse.

**A vendor cancelling an ACCEPTED sub-order still triggers a refund** — `wasConfirmed` in
`cancelInternal` now checks `status == CONFIRMED || status == ACCEPTED`, since ACCEPTED implies
CONFIRMED already happened (payment was confirmed either way). `isCancellable` was extended the
same way: ACCEPTED stays cancellable, matching BR-ORD-13's "before Dispatched" — Accepted precedes
Dispatched in BR-ORD-03's lifecycle, which this codebase still only partially models (see
`OrderStatus`'s own javadoc on why Dispatched/In Transit stay `fulfilment`'s alone).

### A real bug caught by the new test, not a design oversight

`sub_order`'s `sub_order_status_check` CHECK constraint (added in `V5`, extended for DELIVERED in
`V15`) still only allowed `PENDING_PAYMENT, CONFIRMED, DELIVERED, CANCELLED` — adding `ACCEPTED` to
the Java enum without a matching migration meant the very first real `accept()` call failed with a
genuine Postgres constraint violation, surfaced as a 500 by the new test, not by inspection. Fixed
in `V28__sub_order_accepted_status.sql`, the same drop-and-recreate `V15` already used.

**Auto-cancellation's real elapsed-time behaviour is not exercised by an automated test** — the
same limitation this codebase already accepts for `PaymentTimeoutSweeper`, `TaxRetrySweeper` and
`ReservationExpirySweeper`: there is no mutable/test `Clock` bean anywhere in this codebase, and
`autoCancelUnaccepted` is a package-private method with no HTTP path (deliberately — it is a system
action, not a caller-invocable one), so it cannot be exercised without either waiting 48 real
hours or building test infrastructure this pass does not add. `accept`'s real, immediate path is
fully tested; the sweep's wiring is verified by compilation and the Spring context loading it as a
bean, consistent with how the other three sweepers in this codebase are verified today.

**An unrelated, unexplained file corruption was found and fixed in passing**: `payment
.PaymentInitiation.java` had a truncated interface declaration (`public interface PaymentInitiatio`
with no opening brace) that this session never touched — every full build through the `admin`
module's completion compiled it successfully, so this happened between that point and this pass's
first compile. The cause could not be determined from git history (this whole backend is one large
uncommitted working tree); it compiles correctly now.

Verification: `WalkingSkeletonFlowTests.vendorAcceptsAConfirmedSubOrder` exercises accept over real
HTTP — cross-vendor 404, successful acceptance, refused re-acceptance, and a still-cancellable
ACCEPTED sub-order whose cancellation notification carries the accurate reason.

## BR-CHK-05/06/07: checkout↔fulfilment delivery wiring (closing a second deferred gap)

`checkout` already had `fulfilment` in `allowedDependencies` from Phase 4 onward, anticipating
exactly this; only the wiring was ever deferred. No module boundary changes were needed to build
this — `ModularityTests` never had to accept a new edge.

**Two new public interfaces on `fulfilment`, split by read/write** — matching the {@code Orders}/
{@code OrderCreation} split `order` already established: `DeliveryQuotes` (read-only — prices a
zone+method before an order exists) and `DeliverySelections` (write — records the buyer's choice
against a real sub-order id, right after `order.OrderCreation.create` returns one). A quote is
resolved once, in `CheckoutService.checkout`, and never re-resolved later — the same
versioned-value discipline `order` already applies to the commission rate it captures at creation
(COM-02, DAT-07): the exact carrier, rate card and price a `DeliveryQuote` names are what gets
charged and what later gets booked.

**Delivery cost reaches `order` as money only, never as fulfilment's own vocabulary.** `order
.OrderCreation.create` gained a `Map<UUID, Money> deliveryCostByVendor` parameter — `order` still
depends on neither `fulfilment` nor its types, only on knowing each vendor group owes an extra
amount. The zone/method/carrier that produced that amount goes straight into `fulfilment` via
`DeliverySelections.select`, a separate write, not a new field on `OrderPaymentConfirmed` — adding
one there would have rippled into every one of that event's four existing consumers (`inventory`,
`ledger`, `tax`, `notification`) for data none of them need.

**`OrderReadinessListener` now auto-books.** It still creates one `Delivery` row per confirmed
sub-order exactly as before (BR-FUL-01), but immediately reads back the buyer's recorded selection
and calls a new `DeliveryService.autoBook` — no vendor action needed to reach BOOKED any more. A
sub-order with no selection (nothing else in this codebase creates an order any other way, but a
future path might) falls back to the original PENDING, unbooked behaviour.

**One destination zone for the whole order, but delivery method per vendor group** —
`CheckoutRequest.deliveryZoneId` (required) and `deliveryMethodByVendor` (optional, DOOR default).
Not a compromise: `fulfilment.DeliveryRateCard` prices by destination zone and method only, never
origin — Phase 7 never modelled per-vendor origin pricing — so a single destination is what the
priced data actually supports, while method genuinely does vary per vendor group and the request
shape reflects that honestly. BR-CHK-08's saved address book stays unbuilt; the buyer supplies a
zone id every call, the same "submitted every call, not persisted" treatment BR-CHK-03's cart
already gets. "Taxes" in BR-CHK-07's breakdown are not computed — no VAT-at-checkout concept exists
anywhere in this codebase, and the BRD's own VAT-sensitivity note (5.6) leaves this genuinely open.

### A real correctness question the delivery-cost addition surfaced, resolved by tracing the money

Adding delivery cost to `SubOrder.total()` raised a real question: should `SubOrderCancelled
.refundAmount` — which `ledger.LedgerService.postRefund` runs commission math directly against —
also grow to include delivery? Tracing every consumer answered it: `LedgerPostingListener`'s
`postPaymentConfirmed`, `postRefund`'s own commission split, `InvoiceGenerationListener`'s eTIMS
invoice and `CancellationCreditNoteListener`'s credit note all already work in
`SubOrderSettlementDetail.subtotal` — items only, never delivery, because delivery is a
pass-through carrier charge, not vendor revenue, and was never commission-bearing. Growing
`refundAmount` to include delivery would have made ledger charge commission on a courier fee — a
real accounting bug, not a hypothetical one. `SubOrderCancelled.refundAmount` stays items-only,
documented explicitly now as to why; **the money actually returned to the buyer is a different,
correctly-larger figure** — `OrderService.cancelInternal` refunds `SubOrder.total()` (items plus
delivery) straight through `payment.RefundInitiation`, not through the event at all. A buyer who
paid for delivery and had their order cancelled now gets that money back; before this fix, they
would not have.

**A genuine, separate gap this surfaced but does not close:** REG-04's eTIMS invoice, and REG-08's
credit notes, still only ever reflect items — delivery cost is not part of `OrderPaymentConfirmed`
at all, by the deliberate design choice above. Tax compliance for a pass-through delivery fee is a
real, open question this pass does not attempt to resolve.

Verification: a new `checkoutPricesAndAutoBooksDeliveryFromBuyerChoice` test exercises the whole
chain over real HTTP — an explicit PICKUP_POINT choice pricing cheaper than the DOOR default, a
real per-vendor cost breakdown, auto-booking with the buyer's own method, a cancellation refunding
the full items-plus-delivery amount (checked directly against the `refund` table), and a zone with
no rate card refusing checkout outright rather than pricing delivery at zero. The existing
`fulfilmentTracksADeliveryFromBookingToCompletion` test was rewritten to match the new reality —
BOOKED immediately after payment, not PENDING, with a re-booking attempt now correctly refused —
and every other existing checkout call site across the suite (15 of them) picked up a real seeded
delivery zone rather than being defaulted around. Full `./gradlew build` checkpoint below.

## BR-NTF-01: dispatch and delivery notifications (closing a third deferred gap, corrected)

The earlier claim in `notification/package-info.java` — that resolving the buyer for a dispatch or
delivery notification would need `order` on top of `fulfilment::events` — was checked while
building this and turned out wrong. `fulfilment.internal.DeliveryEntity` already carries
`buyerAccountId`, captured off `order.events.OrderPaymentConfirmed` when the delivery row is first
created (`OrderReadinessListener`); it simply was not on `fulfilment`'s public events yet. Adding
it was zero-ripple: neither `DeliveryDispatched` (new) nor `DeliveryCompleted` (existing) had any
consumer before this phase.

**A new event, one existing event extended:** `DeliveryDispatched` is published from both
`DeliveryService.dispatch` (the mock carrier gateway) and `dispatchManually` (a vendor's own
hand-entered tracking reference) — BR-NTF-01's "notify on dispatch" does not care which path got
there, so both do. `DeliveryCompleted` gained a `buyerAccountId` field.

`notification`'s `allowedDependencies` gained a fourth superset entry, `fulfilment::events` —
`notification.internal.DeliveryNotificationListener` resolves the buyer's contact from it exactly
the way every other listener in this module already does, no further dependency needed. BR-NTF-01
is now fully real: order confirmation, dispatch and delivery all send.

Verification: `fulfilmentTracksADeliveryFromBookingToCompletion` was extended with two
`awaitNotificationSent` checks — one after dispatch, one after delivery — proving both new sends
actually reach the admin-visible notification queue, not just that the code compiles.

## BR-SEC-03/04/05: order-origin search and cross-account abuse signals (closing a fourth deferred gap)

BR-SEC-03 (repeated activity from one source address) and BR-SEC-04/05 (buyer abuse / vendor fraud
signals "materially above the platform median") were the last Should-priority items left deferred
from the original BR-SEC pass (above). Both are now real.

**BR-SEC-03 needed no new module.** `order.ParentOrder` gained `sourceAddress`/`userAgent`,
captured once at checkout from the same request `web.RateLimiter`'s BR-SEC-01 check already reads
`sourceAddress` from (`CheckoutController`), and surfaced through a new
`order.Orders.findBySourceAddress`. It was folded into `admin.internal.AdminOrderSearchController`'s
existing BR-ADM-02 search as a fifth criterion — "who else ordered from this address" is exactly the
cross-vendor search that endpoint already provides, unioned with the existing name/email/order-id/
status matches. Nothing about this needed `integrity` at all; it lives entirely in `order`,
`checkout` and `admin`.

**BR-SEC-04/05 could not stay per-account.** "Materially above the platform median" is inherently a
cross-account read — computing a median needs every buyer's or vendor's rate at once, which cannot
be resolved from a single event payload the way BR-NTF-01's dispatch/delivery gap turned out to be
(previous section). `integrity.internal.AbuseDetectionService` is new, and `integrity`'s
`allowedDependencies` grew from empty to `{order, returns, fulfilment, vendor}` — a real, deliberate
superset of ADR-002's table. Each owning module exposes only its own raw counts through a narrow,
read-only interface, and none of those counts are recomputed elsewhere:

- `order.BuyerOrderActivity.cancellationStats()` — per-buyer sub-order and cancellation counts, a
  new JPQL constructor-expression query (`select new ...BuyerCancellationRow(...) from SubOrder ...
  group by buyerAccountId`) on `SubOrderRepository`.
- `returns.BuyerReturnActivity.returnStats()` — per-buyer return and disputed-return counts, same
  pattern on `ReturnRepository`. Its javadoc documents a real limitation: only `RESOLVED_VENDOR`
  returns count as "disputed," since that is the only outcome that actually recorded the buyer
  losing a dispute in this codebase's current state model.
- `fulfilment.VendorDispatchActivity.dispatchStats()` — per-vendor total and failed-delivery counts,
  same pattern on `DeliveryRepository`.
- `vendor.Vendors.vendorsInPayoutCoolingOff()` — reuses BR-VND-09's existing cooling-off window
  as-is; not a new signal, just a new read of one.

`AbuseDetectionService` aggregates these into a median per metric (cancellation rate, return rate,
dispute rate for buyers; dispatch-failure rate for vendors) and flags an account whose own rate is
more than `MATERIALITY_MULTIPLIER` (2×) the platform median. When the median itself is zero — the
common case at low volume, since most buyers never cancel — that comparison is meaningless (anything
above zero times a multiplier is still zero), so `FLOOR_RATE` (0.5) is the fallback: a raw rate above
it is flagged outright regardless of the median. Neither constant is sourced from configuration yet;
both get the same "not yet business-tunable" treatment as `inventory.internal.
InventoryService.RESERVATION_TTL`. A vendor's `payoutRecentlyChanged` (BR-VND-09 cooling-off) flags
it independently of the dispatch-failure comparison — a payout change just before a settlement run is
worth an admin's attention on that basis alone. `AdminAbuseController` exposes both signal lists at
`GET /admin/integrity/buyer-abuse` and `GET /admin/integrity/vendor-fraud`, `PLATFORM_ADMIN`/
`SUPER_ADMIN` only.

**BR-SEC-05's third signal — stock-out rate on accepted orders — is not built.** Unlike dispatch
failure or payout timing, nothing in this codebase tracks "a vendor accepted a sub-order, then could
not fulfil it for lack of stock" as a distinct, queryable event; `OrderService.accept` has no failure
path of that shape. Inventing one to serve a single Should-priority sub-metric was judged out of
scope for this pass. The other two BR-SEC-05 signals (dispatch failure, payout timing) are real.

**`integrity` still has no dependency on `iam`,** even though ADR-002 anticipated one for
account-level pattern detection: nothing built here, including this pass's analytics, ever needs to
resolve an id back to an `iam.Account` — `AbuseDetectionService` works entirely in ids and counts,
and `AdminAbuseController` returns them as-is for an admin to cross-reference elsewhere.

Verification: a new `WalkingSkeletonFlowTests` case drives a buyer through three checkouts against
the same vendor, cancelling every one to force a 100% cancellation rate, then confirms
`GET /admin/integrity/buyer-abuse` flags that buyer (robust against the shared test database's
accumulated median, since most other buyers across the suite never cancel — the platform median is
0, so `FLOOR_RATE` governs) and that a non-admin gets 403 on the same endpoint; confirms
`GET /admin/orders/search?sourceAddress=127.0.0.1` returns all three cancelled orders (BR-SEC-03);
then completes a fourth, fully paid order for the same vendor (needed because `vendorFraudSignals()`
only reports vendors with at least one delivery row) and changes that vendor's payout method,
confirming `GET /admin/integrity/vendor-fraud` reports both `payoutRecentlyChanged` and `flagged`
for it. Targeted regression (`WalkingSkeletonFlowTests`, `ModularityTests`, `AuthenticationFlowTests`,
`InventoryConcurrencyTests`) is clean on fresh XML: `failures="0" errors="0"` across all four
classes, `ModularityTests` confirming the new `integrity → order, returns, fulfilment, vendor`
dependency graph introduces no cycle.

## BR-ADM-06: holdback rate and settlement cycle configuration (closing a fifth deferred gap)

The two BR-ADM-06 values left unversioned when `admin` shipped — holdback percentage and
settlement cycle — are now real. BRD section 15.2 is explicit that both determine an amount owed
and must be "reconstructible as at any past order," the same requirement COM-05 already imposed on
commission rate; DAT-07 says the same thing as a data-quality rule. The two values turned out to
need different treatment, not the same one, once that requirement was taken seriously for each.

**Holdback rate joined commission rate in `pricing`, resolved and stored the same way.** `pricing.
HoldbackRate` is `CommissionRate`'s shape exactly; `PricingConfiguration` gained
`currentHoldbackRate`/`holdbackRateForVersion`, `PricingAdministration` gained
`createHoldbackConfiguration`, and a new `holdback_configuration` table (`V32`) is versioned the
same way `commission_configuration` (`V10`) always was — an immutable row per change, an
effective-from timestamp, never an `UPDATE`. `order.OrderService.create` resolves
`currentHoldbackRate` once per order, alongside `currentCommissionRate` (COM-01/02/05's "one
configuration version per order, not per vendor group within it" applies to both identically), and
stores the returned version id on a new `sub_order.holdback_configuration_id` column (`V33`) — the
same COM-05 mechanism, one column over. `ledger.internal.LedgerService.postPaymentConfirmed`/
`postRefund` take `holdbackRate` as a parameter now, not a hardcoded `BigDecimal("0.10")`; the
resolved rate reaches `ledger` the same way commission rate always did — carried on
`OrderPaymentConfirmed.SubOrderSettlementDetail`, `SubOrderCancelled` and `returns.events.
ReturnRefunded`, since `ledger` depends only on those modules' `::events`, not on `pricing`. No
module's `allowedDependencies` changed: the versioning machinery already existed in `pricing`,
`order` already depended on it, and `ledger`/`returns` already carried commission rate the same
way — holdback rate is additional fields on types that already made this exact trip.

**Settlement cycle did not join them — a real, deliberate difference, not an oversight.**
Commission and holdback are facts about a *specific order*: COM-05 needs to reproduce what applied
when that order was created, which is why a version id is stored on the sub-order forever. A
settlement cycle is not a fact about an order at all — BRD section 5.6 frames it as "how often
[vendor] disbursement happens," a scheduling policy with no order to attach to. Routing it through
`pricing` and `order` the way holdback was would misrepresent it as the wrong kind of fact.
Instead `settlement.SettlementCycle` (`configurationVersionId`, `cycleDays`) is versioned entirely
inside `settlement` — a new `settlement_cycle_configuration` table (`V34`, `cycle_days` constrained
to 1–31 per BRD 5.6's "must support configuration between daily and monthly"; OPEN-04 records the
actual frequency as not yet agreed by Finance, so 7 is only a seeded starting default) — and
`SettlementService` resolves whichever configuration is *current* at the moment a run is
calculated, deliberately, the same way `Vendors.payoutCoolingOffUntil` is checked fresh on every
run rather than captured once. No new module dependency: `SettlementController` is already
`settlement`'s own admin-facing surface (`POST /settlement/config/cycle`, `FINANCE_ADMIN`/
`SUPER_ADMIN`), the same "admin lives in the owning module" precedent `fulfilment.internal.
AdminFulfilmentController` set for delivery zones and rate cards; `AuditLog` is the OPEN-module slot
`pricing.internal.PricingService` already used, needing no new dependency either.

**The cycle is enforced, not just recorded — per vendor, not as a single lock on the whole run.**
`SettlementService.exclusionReasonFor` gained a fourth check, `SETTLEMENT_CYCLE_NOT_ELAPSED`,
alongside `MANUAL_HOLD`/`NO_PAYOUT_DETAILS`/`PAYOUT_DETAILS_COOLING_OFF`: a vendor is excluded from
a run if less than the current `cycleDays` has elapsed since that vendor's own most recent
`DISBURSED` settlement line. Deliberately per vendor rather than one lock on `initiate` for the
whole currency — a global lock would make a vendor's very first settlement wait out a cycle that
had not started for it, and, in the shared, non-rolled-back test database this session's tests all
run against, would have coupled this test's pass/fail to whichever other test happened to run a
settlement for the same currency first. A fresh vendor with no prior `DISBURSED` line is never
excluded by this check, which is what keeps `settlementRunDisbursesEligibleVendorAndExcludesIneligibleOne`
(a vendor's first-ever settlement) unaffected by this addition.

Verification: a new `WalkingSkeletonFlowTests.settlementCycleExcludesAVendorSettledTooRecently`
settles one vendor once, immediately generates a second payable balance for the same vendor, and
confirms a second run excludes it with `SETTLEMENT_CYCLE_NOT_ELAPSED` under the default seeded
7-day cycle; then backdates that vendor's `resolved_at` past 7 days (the same real-time-test
backdating stand-in `payout_detail.cooling_off_until` already uses elsewhere in this file) and
confirms a third run includes and disburses it — proving the exclusion is real in both directions,
not a one-way permanent block. `adminToolsAreRealAndAudited` was extended with the holdback-rate
and settlement-cycle admin endpoints: a real versioned change (far-future `effectiveFrom`, the same
"prove the mechanism, don't move the live default" discipline the existing commission-rate
assertion already followed, after the commission-rate test-pollution bug this same file documents
above), a `FORBIDDEN` check for a non-finance caller on each, and a `BAD_REQUEST` check for each
value's out-of-range validation (holdback rate ≥ 1; cycle days outside 1–31).

Full `./gradlew build` checkpoint: the first run failed one unrelated, pre-existing test —
`repeatedOtcRequestsAreRateLimitedAndEscalateToARestriction` got `200` where it expected `429`.
`RateLimiterService`'s own javadoc already documents why: it is a fixed window aligned to wall-clock
time ("a window boundary can let two requests land close together across it, which BR-SEC-01/02 do
not require this precisely"), not a window relative to first use — this phase's larger test file
shifted exactly when that test's requests fall relative to a minute boundary, an existing, accepted
trade-off surfacing by chance rather than a regression in this pass's own code, since the check it
exercises (`SCOPE_OTC_ISSUANCE_INTERVAL`, keyed by a phone number unique to that test) shares no
state with anything BR-ADM-06 touched. Rerun with no code changes: clean. All 9 XML files fresh,
**47 tests total, 0 failures, 0 errors**, summed directly from the XML rather than trusted from the
marker — the same verification discipline applied to every checkpoint in this document.

## BR-ADM-08: admin impersonation of a vendor account (closing the sixth and last deferred gap)

BR-ADM-08 is now real: an admin can mint a genuine, short-lived session that authenticates as a
vendor's own owner account, for support purposes, and — the load-bearing half the earlier note
above warned against skipping — it is both audited and visible to the vendor. The two halves live
in two different modules, deliberately.

**`iam.ImpersonationSessions` owns the session mechanics, because `iam` already owns every
session.** Standing up a second, parallel way to become authenticated as an account — even a
well-intentioned one, scoped to admins — is exactly the kind of duplicate authority path this
codebase's module boundaries exist to prevent. `issue` reuses `AuthenticationService`'s existing
machinery end to end: the same `TokenHashing.generateToken`/`hash`, the same `session_token` table
(a new nullable `impersonator_account_id` column, `V35`, distinguishes an impersonation session
from an ordinary one), the same auth-event trail. The one real difference is validity: 30 minutes,
not the ordinary 30 days — a support session, not a login, expressed as data rather than as a
special case some other layer has to remember to enforce.

**`iam` still does not know what a vendor is, and does not need to.** BR-ADM-08's subject is
specifically "a vendor account", which means something has to resolve a vendor id into the account
id actually impersonated — but `iam`'s `allowedDependencies` cannot grow a `vendor` entry without
creating a cycle, since `vendor` already depends on `iam`. `vendor.internal
.VendorImpersonationService` is that resolver: it turns `vendorId` into `VendorEntity
.ownerAccountId()` and calls `ImpersonationSessions.issue`, the same "a narrow interface trusts its
caller to enforce its own scope" pattern `web.AuditLog` already established for BR-ADM-07 — `iam`
trusts `vendor` to have checked the caller is really `PLATFORM_ADMIN`/`SUPER_ADMIN` and to be
naming a real vendor's real owner, the same way `AuditLog` trusts every caller to describe its own
action truthfully rather than re-deriving it. `vendor.internal.VendorImpersonationController`
carries both `POST /admin/vendors/{vendorId}/impersonate` (the admin action) and `GET
/vendors/{vendorId}/impersonation-log` (the vendor-visible read) in one file, even though their
paths sit under different prefixes — one feature, reviewed as one feature, matching every other
"admin lives in the owning module" precedent in this codebase rather than waiting on a future
`admin`-module aggregation.

**"Visible to the vendor" is owner-gated, not staff-gated** — the same restriction
`PayoutDetailController`'s `AddStaffRequest` already gives owner-level actions. Whether an
account has been impersonated is the vendor principal's own security posture to see, not a
delegated staff permission, so `VendorImpersonationService.history` checks `VendorMembershipService
.relationshipOf(...) == Relationship.OWNER` before calling `ImpersonationSessions.historyFor`.

**What this pass deliberately does not build: per-request attribution.** Once an impersonation
session is issued, every module downstream sees an ordinary `AuthenticatedCaller` for the target
account — indistinguishable, for the rest of that session's life, from the vendor's own real
session. `impersonatorAccountId` is recorded once, at issuance, in the audit log and in the session
row `historyFor` reads back. That is what BR-ADM-08's own text asks for — "impersonation... logged
and visible to the vendor" — not a system that tags every individual write made through the session
back to the admin who opened it, which would touch every module's request handling for a
requirement the BRD itself marks Could-priority. Recorded here as a deliberate boundary, the same
way BR-SEC-05's stock-out signal and BR-ADM-06's per-request settlement itemisation are recorded as
deliberate boundaries elsewhere in this document, not gaps discovered by accident.

Verification: a new `WalkingSkeletonFlowTests.adminImpersonatesAVendorAccountRealAuditedAndVisible`
confirms a non-admin gets `FORBIDDEN` starting an impersonation; a real admin gets back a token that
genuinely authenticates as the vendor's owner account, proven by successfully reading a
vendor-owner-gated endpoint (`GET /vendors/{vendorId}/payout-details`) with nothing but that token;
`GET /admin/audit-log` shows a real `VENDOR_IMPERSONATION_STARTED` entry against the vendor's
account id (BR-ADM-07); the vendor's own `GET /vendors/{vendorId}/impersonation-log` shows a record
naming the impersonating admin's account id (BR-ADM-08's own visibility requirement); and an
unrelated caller gets `FORBIDDEN` reading that same log. `ModularityTests` confirms `iam`'s
`allowedDependencies` did not grow — the cycle risk this design was built specifically to avoid.

## Two real bugs found by running the application, not by testing it

Every checkpoint in this document up to this point was verified through `@SpringBootTest` — real
HTTP, a real Postgres via Testcontainers, but always inside a test context. Actually booting the
application (`./gradlew bootTestRun`, the real `MeridianApplication` main class plus
`TestcontainersConfiguration` for the database) surfaced two real, previously-undiscovered bugs in
minutes that the full automated suite — every module, every phase, this entire document — had
never once caught, because both bugs live specifically in the gap between "what a test exercises"
and "what a running application does."

**Bug 1: the application could not start at all outside a test.** `application.yaml`'s tracing
config set `management.opentelemetry.tracing.export.otlp.endpoint: ${OTLP_TRACES_ENDPOINT:}` — an
empty string when the env var is unset, which is the case in local/default runs. Spring's
`@ConditionalOnProperty` on that key treats an empty string as "present," so `spring-boot-
micrometer-tracing-opentelemetry`'s `OtlpTracingConfigurations.ConnectionDetails` bean always
activated, and `Exporters.otlpHttpSpanExporter` then failed to even construct —
`OtlpHttpSpanExporterBuilder` rejects an empty endpoint outright ("Invalid endpoint, must start
with http:// or https://") — crashing `ApplicationContext` startup. `@SpringBootTest` disables
tracing autoconfiguration by default (Spring Boot's own `ObservabilityContextCustomizerFactory`),
so nothing in this codebase's test suite — including every `WalkingSkeletonFlowTests` method that
boots the full context — ever reached this code path. Fixed by defaulting the endpoint to
`http://localhost:4318`, OTel's own conventional local-collector address: syntactically valid, so
the exporter bean constructs and startup succeeds; if nothing is listening there (true for a bare
local run), individual span exports fail silently in the background rather than blocking startup —
which is what the original comment ("nothing is exported and startup is unaffected") always meant
to describe, just implemented with a value that made the opposite true.

**Bug 2: registration and login were not completable by an actual user.** Tracing a live
`POST /api/auth/register` call to its mock-SMS log line showed the SMS body read *"Your Meridian
verification code request \<uuid\> expires at \<time\>."* — no code. `iam.internal
.AuthenticationService.issueCode` generates the plaintext code, BCrypt-hashes it into the OTP
challenge, and discarded the plaintext immediately after — the `OneTimeCodeIssued` event it
published deliberately never carried the code (a real, correct security choice: a plaintext
credential sitting in the event publication registry table is a credential at rest), only a
`deliveryReference`. Nothing downstream could ever resolve a code from that reference, because the
only stored form is an irreversible hash. `notification.internal.OneTimeCodeNotificationListener`
therefore sent a real SMS that never had a code to include. Every automated test used
`CapturingOneTimeCodeGenerator`, a test-only double that intercepts the plaintext code before `iam`
hashes and discards it — which correctly exercises verification, but never exercises delivery, so
this was invisible to the entire suite. Fixed by removing the event path entirely: `web
.MessageGateway` (new, filled by `notification.internal.MessageGatewayAdapter`) is a synchronous
slot `AuthenticationService.issueCode` calls directly, in the same method, while the code is still
a local variable — the one deliberate exception to ADR-004's "events, not direct calls, for
notification," documented in both `web.MessageGateway`'s javadoc and a new ADR-004 note, because
there is no event-shaped way to keep the plaintext off a persisted, retried table and still get it
to the user before it matters. `iam::events` and the now-pointless `OneTimeCodeIssued` type are
deleted outright rather than left as dead code; `notification`'s `allowedDependencies` lost the
`iam::events` entry it no longer needs.

**Verification for both was live, not just automated**, because the whole point was that automated
verification had already passed while both bugs were present. After each fix: rebuilt, relaunched
via `bootTestRun`, confirmed `GET /actuator/health` → `{"status":"UP"}` (bug 1), then ran a real
`POST /api/auth/register` against the running server, read the actual mock-SMS log line — *"Your
Meridian verification code is 128615. It expires at ..."* — extracted the code exactly as a human
reading their phone would, and posted it to `POST /api/auth/verify`, which returned a real session
token, the correct account id, `["BUYER"]`, and a 30-day expiry (bug 2). Only after both live
checks passed was the automated suite re-run: `ModularityTests` clean (confirming `iam`'s
dependency graph still has no cycle and did not grow to reach `notification`), the full targeted
regression (`WalkingSkeletonFlowTests`, `ModularityTests`, `AuthenticationFlowTests`,
`InventoryConcurrencyTests`) clean on fresh XML, `failures="0" errors="0"` throughout.

The lesson this leaves for future work in this codebase: `@SpringBootTest` coverage, however
thorough, is not proof an application actually runs or that a feature is actually reachable by a
real user — both of those need the application itself to have been started at least once, which,
until this pass, it never had been.
