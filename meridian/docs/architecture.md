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

| Module | Owns | BRD | Phase |
|---|---|---|---|
| `shared` | `Money`, cross-cutting value types | DAT-01 | 0 |
| `pricing` | Commission rates, holdback, settlement cycle, return window — all versioned | COM-01 to 05 | 0, 3 |
| `iam` | Registration, roles, authentication | BR-IAM | 1 |
| `vendor` | Applications, verification, payout details, suspension | BR-VND | 2 |
| `catalog` | Products, variants, categories, price history | BR-CAT | 1, 2 |
| `inventory` | Stock, reservations, movements | BR-INV | 2 |
| `order` | Parent orders, sub-orders, state machine | BR-ORD | 1, 4 |
| `checkout` | Cart, delivery selection, checkout orchestration | BR-CHK | 4 |
| `payment` | Collection, refunds, disbursement, reconciliation | BR-PAY | 3 |
| `ledger` | Double-entry ledger, trial balance | BR-LDG | 3 |
| `settlement` | Entitlement, holdback release, payout runs | BR-STL | 6 |
| `returns` | Returns, refunds, disputes | BR-RTN | 5 |
| `fulfilment` | Carriers, zones, rate cards, tracking | BR-FUL | 7 |
| `tax` | eTIMS invoices and credit notes | REG-04 to 08 | 3 |
| `review` | Reviews and ratings | BR-REV | 8 |
| `search` | Keyword search, filtering, sorting | BR-SCH | 8 |
| `notification` | SMS and email delivery, retry | BR-NTF | 2+ |
| `integrity` | Rate limiting, abuse and fraud signals | BR-SEC | 9 |
| `admin` | Back-office queues and configuration screens | BR-ADM | 2+ |

The full dependency table is in ADR-002.

## Rules

1. **Notify by event, query by call.** Cross-module notification uses domain events consumed with
   `@ApplicationModuleListener`. Direct dependencies are for synchronous questions only. ADR-004.
2. **Money is `Money`.** Minor units, explicit currency, explicit rounding at every call site.
   No `double`, no bare `BigDecimal` in a monetary field. ADR-003.
3. **Never read current configuration for an existing order.** Orders store the configuration
   version in force at creation and resolve rates through it. ADR-005.
4. **No provider type escapes its adapter.** External providers sit behind interfaces owned by the
   consuming module, in its vocabulary. ADR-006.

## Decisions

| ADR | Decision |
|---|---|
| [ADR-001](adr/ADR-001-modular-monolith.md) | Modular monolith on Spring Modulith |
| [ADR-002](adr/ADR-002-module-boundaries.md) | Module boundaries and the dependency graph |
| [ADR-003](adr/ADR-003-money-representation.md) | Money as minor units with explicit currency |
| [ADR-004](adr/ADR-004-inter-module-events.md) | Inter-module communication by domain event |
| [ADR-005](adr/ADR-005-versioned-configuration.md) | Versioned configuration for money-determining parameters |
| [ADR-006](adr/ADR-006-external-provider-abstraction.md) | External providers behind internal interfaces |

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

## Pipelines

`.github/workflows/backend.yml` and `frontend.yml`, documented in `.github/workflows/README.md`.
**Neither has ever run: the repository has no git remote.**

## Not yet done

- **Deployment.** No cluster, registry or staging environment exists, so the second half of the
  Phase 0 exit criterion — "pipeline deploys to staging automatically" — is not met.
- **Rounding convention per use** — commission, VAT, holdback — recorded as a business decision
  (ADR-003). It moves money, so it is not an engineering choice.
