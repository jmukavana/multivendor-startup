---
title: "Project Meridian BRD v2.1 — Technical and Commercial Review"
subtitle: "Findings against the document issued for client sign-off"
date: "31 August 2026"
lang: en-GB
---

## Purpose and status

This is a review of `project-meridian-brd-v2.1.md` (v2.1, 31 August 2026, issued for client
sign-off). It records defects found in the document, not in the platform. Findings are ordered
by the damage they would do if the document reached a client or an investor unchanged.

Classification follows the source document: **Confidential — for named recipients only.**

**Reviewer's note on scope.** Section 9.13 (use case to requirement traceability) was not
verified mapping by mapping. The legal citation in 14.1 and the M-Pesa transaction limit in
BR-PAY-08 were not independently confirmed and are flagged for verification rather than
disputed.

| Severity | Count | Meaning |
|---|---|---|
| Critical | 5 | Wrong or self-contradicting on a number or claim a client will check |
| Major | 9 | Structural: changes what gets built, or what is being signed |
| Minor | 10 | Gaps and inconsistencies that should close before sign-off |

---

## Critical

### C-01 — The single-order unit economics do not add up (5.3)

The table states gross platform revenue of 256 (216 commission + 40 processing fee), then
deducts 78 aggregator, 260 carrier, 4 SMS and 12 disbursement. That sums to **-98**, which the
table gives correctly. It then adds "delivery margin retained 40" and arrives at **142**.

    -98 + 40 = -58, not 142.

The table is out by 200 at the line on which the whole of Section 5 rests.

The cause is visible in the structure: the 300 delivery charged to the buyer is included in
"total buyer payment" but excluded from "gross platform revenue", while the 260 carrier cost is
deducted in full. Delivery is counted as a cost with its revenue omitted, and then a 40 margin
is added back on top — an error in two directions at once. Computed correctly the figure is
**202** before returns, not 142 and not -58.

Everything downstream of 142 is internally consistent (142 - 18 = 124), so the defect is
contained to one step, but 124 propagates into 5.4 LTV, 5.7 contribution and 5.8 funding.

Corrected model: see `brd-v2.1-section-5-corrected.md`.

### C-02 — The executive summary contradicts the projection on profitability

Section 1.3 states **"Projected monthly breakeven: Month 22 post-launch."**

Section 5.7 states **"The projection does not reach profitability within three years on these
assumptions"**, and shows EBITDA worsening every year: -20.8M, -23.8M, -26.0M.

These cannot both stand. The summary is the only part many readers will read, and it asserts
the opposite of the model. Either substantiate Month 22 with a model that produces it, or
delete the line.

### C-03 — The funding requirement is not derived from the burn (5.8, 1.3)

Section 5.8 requests 9.2M of working capital for "12 months post-launch". Section 5.7 projects
Year 1 EBITDA of **-20.8M**. Cumulative three-year EBITDA is **-70.7M** against a total ask of
23.57M.

The working capital line is roughly 44% of the first year's burn as the document itself models
it. On the corrected model (C-01) Year 1 burn is 18.6M and 9.2M funds approximately **six**
months post-launch, not twelve. The ask should be restated as funding build plus a defined
runway, with the runway stated in months and the breakeven claim removed.

### C-04 — Two of the eleven business objectives are unreachable against the model (3, 5.7)

| Objective | Target at 12 months | Model's Year 1 exit run rate | Status |
|---|---|---|---|
| OBJ-01 active vendors | 250 | 250 | Consistent |
| OBJ-02 monthly GMV | KES 15,000,000 | 3,500 x 2,400 = **8,400,000** | 44% short |
| OBJ-03 monthly net commission revenue | KES 1,200,000 | 3,500 x 256 = **896,000 gross** | Short before netting |

The order count is the document's own: 250 vendors x 14 orders per vendor per month (5.4).
OBJ-01 ties to the model exactly, which indicates the objectives were derived from it and two
of them subsequently drifted. OBJ-02 requires either 446 vendors or an average order value of
4,286. OBJ-03 is unreachable even before variable costs are netted off.

Objectives that the company's own financial model says cannot be met are worse than no
objectives, because OBJ-02 failing for two consecutive quarters triggers a strategy review
under Section 21 — a review that would be triggered by a target error, not by performance.

### C-05 — The VAT leakage assumption is load-bearing and undisclosed (5.6, 5.7)

Section 5.6 correctly models the gap between classifications on a representative order:
VAT of 35 as agent against 372 as principal — a difference of 337, or approximately **14% of
GMV**.

Section 5.7 then assumes a blended leakage of **3.2% of GMV**, which implies roughly 77% of the
exposure is mitigated away. No derivation is given: no vendor mix, no VAT-registered share, no
recovery rate.

This is the most consequential number in the document and the only major one with no working
shown. Sensitivity, on the corrected model:

| Blended leakage | Year 3 contribution | Year 3 EBITDA |
|---|---|---|
| 3.2% (as assumed) | 19.7M | -12.2M |
| 8.0% | -3.7M | -35.5M |
| 14.0% (unmitigated principal) | -32.9M | -64.7M |

The document names this as RSK-01 and as its largest risk. A single blended figure is not an
adequate treatment of the largest risk in the model. Replace with a sensitivity table and state
the vendor mix that produces the chosen figure.

---

## Major

### M-01 — Escrow and payment licensing are in unresolved conflict (5.9 vs 14.3)

REG-09 and BR-PAY-01 require that the platform shall not collect, hold or disburse customer
funds without a CBK licence, and that all collection and disbursement run through a licensed
PSP.

Section 5.9 then describes funds being "collected by the payment aggregator into a
**platform-controlled account**", from which the platform calculates entitlement, retains a
holdback, and instructs disbursement.

An account the platform controls, holding buyer money pending settlement, with the platform
directing payout, is substantively the activity REG-09 prohibits. The aggregator's licence
covers the aggregator's activity; it does not automatically extend to an account under the
platform's control. This is the structure that attracts CBK scrutiny, it is the mechanism the
entire buyer-trust proposition depends on, and it is **not recorded as an open issue**.

**Action.** Raise as OPEN-10, owner Legal, required before Phase 0 exit, and put it to counsel
alongside DEP-02. The answer determines whether the holdback mechanism in 5.9 and BR-STL-01 is
available at all.

### M-02 — The requirements have already decided the question the document says is open (10 vs 5.6, 14.1)

Section 14.1 identifies the four factors that produced principal classification in the cited
judgment: setting prices, dispatching providers, issuing payment requests as tax invoices, and
collecting payment. Section 5.6 offers "structural agency" as mitigation (2) — restricting
transactional control to support agent classification.

Section 10 specifies all four factors as Must-have requirements:

| Factor | Requirement |
|---|---|
| Platform sets price | 5.2 commission with vendor and product overrides |
| Platform prices delivery | BR-CHK-06, BR-FUL-08 |
| Platform dispatches | BR-FUL-01, BR-FUL-03 |
| Platform issues tax invoices | REG-04 |
| Platform collects | BR-PAY-01, 5.9 |

Mitigation (2) is therefore not available: the requirements eliminate it before the tax opinion
that OPEN-02 calls for exists. The document presents a live three-way decision while having
already committed to principal classification and to mitigation (1) alone.

Compounding this, mitigation (1) — onboarding only VAT-registered vendors — requires vendors
above the KES 5M registration threshold, which Section 4.5 identifies as **not** the primary
target segment. The commercial strategy and the target segment are in direct tension and the
document does not resolve it.

**Action.** State plainly that the requirement set assumes principal classification, and that
mitigation (2) would require re-baselining Section 10. Then the client is signing off on a
decision knowingly rather than by implication.

### M-03 — OPEN-06 has already been answered by the requirements, in the worse direction

OPEN-06 asks "whether the platform sets delivery pricing, or vendors do", marks it "directly
affects OPEN-02", and sets Required-by at **Phase 4**.

BR-CHK-06 (platform calculates delivery cost) and BR-FUL-08 (admin configures zones and rate
cards) have already answered it — in the direction that strengthens principal classification.
And OPEN-02, which OPEN-06 affects, is due before **Phase 0 exit**, four phases earlier.

An open issue cannot be due after the issue it feeds. Move OPEN-06 to Phase 0 exit and resolve
it with OPEN-02, or drop the pretence and record the decision as made.

### M-04 — The roadmap inverts a hard dependency (18)

Section 1.3 defines launch as Phases 0-6. Returns, disputes and the return-window mechanism
land in **Phase 7**, after launch.

Phase 5 settlement cannot function without them:

- BR-STL-04 withholds settlement on orders still within the return window
- BR-STL-05 releases holdback once the return window closes without a claim
- 17.3 begins the settlement flow with "return window closes > holdback released"

Phase 5 depends on Phase 7. Separately, Section 6.1 places returns, disputes, reviews and
search in scope, so launch as defined ships without four in-scope capabilities, and Phase 8
hardening (rate limiting, fraud controls, restore testing) also falls after launch.

**Action.** Move the return window, return acceptance and holdback release into Phase 5, or
move settlement to Phase 7. Phase 8 hardening should precede launch, not follow it.

### M-05 — Sign-off is requested on sections the document says cannot be finalised

Status is "Issued for client sign-off", and the signature block asks each party to confirm that
Sections 9 through 16 "represent an accurate and complete statement of the agreed scope".

Against that:

- ASM-05 states the catalog schema and commission model cannot be finalised until OPEN-01 closes
- OPEN-01 (category) is open, Required-by Phase 2
- 5.7 states the business case is contingent on OPEN-01 and OPEN-02, "not on execution of the
  requirements in this document"

The disclaimer in 5.7 helps but does not repair a signature block asserting completeness over
sections that are explicitly incomplete.

**Action.** Either narrow the sign-off to Sections 9-13 and 15-16 with 10.3 and 5.2 marked
provisional pending OPEN-01, or gate sign-off on closure of OPEN-01 and OPEN-02.

### M-06 — Order creation timing is specified twice, differently

| Source | Behaviour |
|---|---|
| BR-ORD-01 | Parent order created **on payment initiation** |
| BR-ORD-03 | Includes a "Pending Payment" state, consistent with the above |
| 17.2 | "payment confirmed > stock deducted > parent order created" |

The two are not reconcilable, and the choice changes the reservation model, the refund path and
the reconciliation logic. BR-ORD-01 plus BR-ORD-03 is the sounder reading — an order record
must exist for a payment callback to attach to — in which case 17.2 should be corrected.

### M-07 — Settlement has no idempotency requirement (10.9)

BR-PAY-05 correctly requires payment callbacks to be processed idempotently, keyed on the
provider transaction identifier.

BR-STL-07 requires failed disbursements to be retried and escalated, with **no corresponding
idempotency requirement**. Double-disbursement is the highest-value failure mode in the system:
it moves real money out of a platform-controlled account to a party with no incentive to report
it, and it is the failure most likely to occur under exactly the conditions BR-STL-07
describes — retry after an ambiguous timeout.

**Action.** Add BR-STL-12: disbursement instructions shall carry a platform-generated
idempotency key, and the system shall guarantee that a settlement line can be disbursed at most
once regardless of retry.

### M-08 — MoSCoW provides no prioritisation signal (10, RSK-12)

Section 10 contains **137 requirements: 119 M, 16 S, 2 C, none W**. Four modules — Orders,
Ledger, Settlement and Cart/Checkout — are Must in their entirety.

RSK-12 rates scope expansion as High impact, High likelihood, and names "strict MoSCoW
enforcement" as the primary mitigation. A prioritisation in which 87% of requirements are Must
cannot be enforced against. The stated mitigation for the highest-likelihood delivery risk is
neutralised by the requirements themselves.

**Two things are needed, and re-labelling alone is not enough.** A marketplace handling money
genuinely does have a large mandatory core: most of Payments, Ledger, Settlement and Orders
cannot be deferred without either losing money or breaking the law. The defect is not that the
Must count is high, it is that MoSCoW is carrying a question it cannot answer — *when*.

1. Define M against a stated launch point rather than in the abstract, and move the genuinely
   deferrable requirements out. Candidates: BR-CAT-09, BR-CHK-03, BR-PAY-08, BR-SCH-02,
   BR-ADM-05, and the reviews module (BR-REV-01, 02, 03, 05, 06) as a block.
2. Add a delivery phase per module, so a Must in a late phase cannot be pulled forward without
   a change request under Section 21. That is what actually constrains scope; priority alone
   never did.

**Correction to an earlier draft of this review**, which proposed moving BR-CAT-10, BR-CAT-12,
BR-VND-11 to 13, BR-INV-07, BR-FUL-05, BR-FUL-07, BR-REV-04, BR-SCH-05, BR-SCH-06, BR-NTF-03
and BR-ADM-08 out of Must. Every one of those is **already** S or C in v2.1. The re-baseline has
to come from the M set, which is what the list in point 1 above does.

### M-09 — NFR-03 and NFR-13 are mutually inconsistent

NFR-03 targets 99.5% monthly availability, which permits approximately **3.6 hours** of
unplanned downtime per month.

NFR-13 sets a recovery time objective of **4 hours**.

A single invocation of the disaster recovery target breaches the availability target in one
event, before any other incident in the month. Either raise availability to 99.9% with a
correspondingly lower RTO, or lower RTO to under 2 hours, or state that DR events are excluded
from the availability calculation.

---

## Minor

### m-01 — Take rate drifts through the projection without explanation (5.7)

| Year | Stated gross revenue | Computed at 9% + 40/order | Variance | Implied take rate |
|---|---|---|---|---|
| 1 | 6,272,000 | 6,272,000 | 0 | 10.7% |
| 2 | 23,304,600 | 22,149,000 | +1,155,600 | 11.0% |
| 3 | 55,926,800 | 50,043,880 | +5,882,920 | 11.5% |

Year 1 ties exactly, which shows the method. Years 2 and 3 do not. If the difference is
REV-03 through REV-05 phasing in per 5.1, the table must show them as separate lines; as it
stands it reads as an arithmetic error, and the Year 3 revenue mix in 5.1 (commission 74%) does
not reconcile with it either — 43.8M of commission against 55.9M of revenue is 78%.

### m-02 — "4.6% of GMV" uses the wrong denominator (5.3)

124 / 2,400 = 5.2% of GMV. The 4.6% figure is against total buyer payment of 2,700. GMV is
defined in the glossary as the value of goods sold. The error sits in the sentence beginning
"Read this carefully."

### m-03 — The incidence of the order processing fee is never stated (5.1, 5.3)

REV-02 is a "fixed fee per order" of 40. Total buyer payment in 5.3 is 2,700 = 2,400 + 300,
which excludes it, so the fee must fall on the vendor — but this is never said. It matters in
two places: vendor effective cost becomes 10.7% rather than 9%, and if the fee is instead
charged to the buyer it forms part of the taxable supply and changes the VAT base.

### m-04 — Cash on delivery is neither in scope nor out of scope

Section 4.4 notes most listed carriers support COD collection, and COD is a dominant mode in
this market. It appears nowhere in 6.1, 6.2 or 10.7. Under Section 6's own rule — "anything not
stated here is out of scope" — it is excluded by silence, which is a significant product
decision made by omission. It also interacts directly with M-01, since COD means the carrier
holds customer funds.

### m-05 — Buyer-initiated cancellation is unspecified (10.6)

BR-ORD-06 covers partial cancellation without naming the actor. BR-ORD-05 covers vendor
non-acceptance. BR-ORD-11 covers admin intervention. BR-ORD-12 covers post-payment
sub-order cancellation. A buyer cancelling before dispatch — high frequency, and the first
thing support will be asked to do — has no requirement.

### m-06 — Fraud controls and rate limiting have no requirements

Phase 8 lists "rate limiting, fraud controls" and RSK-08 mitigates buyer fraud with
"account-level pattern detection". Neither appears as a BR- requirement in Section 10, so under
Section 6 both are out of scope while the roadmap and risk register assume they will be built.

### m-07 — Section 15.1 omits entities that Section 10 requires

Missing from the core entity list: Address, Pickup Point, Carrier, Delivery Zone, Rate Card,
Payout Detail, Commission Rate. All are mandated by BR-CHK-08, BR-FUL-08, BR-VND-08 and 5.2.

Most significant is the last: COM-05 requires the system to reproduce, for any historical order,
"the exact rate applied and the configuration version that produced it". That implies versioned
commission configuration as a persisted, first-class entity with its own lifecycle. Nothing in
Section 15 supports it, and it is not a detail that can be retrofitted cheaply.

### m-08 — Erasure and immutable retention collide unreconciled (14.4, 15.3, NFR-10, DAT-05)

REG-13 requires support for erasure requests. NFR-10 and DAT-05 require immutable, append-only
financial and audit records retained for seven years. Section 15.3 gives buyer personal data
"duration of account plus 2 years, subject to erasure requests" while transaction records are
held for seven.

An erasure request touching an order cannot be honoured under those constraints as written.
The standard resolution — pseudonymise personal identifiers within retained financial records
while preserving the transaction — must be stated as a requirement, not left as a collision
between two sections that a developer resolves by guessing.

### m-09 — NFR-04 has no stated architecture and a thin infrastructure budget

NFR-04 requires 10,000 concurrent buyers and 500,000 catalog items "without architectural
change", alongside BR-SCH-05 (misspelling tolerance) and BR-SCH-02 (faceted filtering). Section
13 lists no search infrastructure and search does not appear until Phase 7. Postgres full-text
will carry this for a period, but "without architectural change" is a strong claim to make while
the search approach is unstated.

Related: 5.5 budgets 145,000 KES per month for GKE, database, storage and egress. That is thin
for a multi-zone cluster meeting 99.5% availability with a 15-minute RPO.

### m-10 — The marketing budget contradicts OBJ-10

5.5 allows 350,000 KES per month for marketing. At OBJ-10's blended CAC ceiling of 450, that
acquires approximately 780 buyers per month. Sustaining 3,500 orders per month at a 30%
90-day repeat rate requires on the order of 2,000 or more new buyers monthly. The budget is
roughly three times short of what the document's own CAC target implies.

---

## Items requiring external verification

| Ref | Item | Why it matters |
|---|---|---|
| V-01 | BR-PAY-08 cites the M-Pesa per-transaction limit as KES 150,000 | Believed to have been raised; the requirement is written around the specific number. Confirm against Safaricom's published limits |
| V-02 | 14.1 cites *Commissioner of Domestic Taxes v. Sendy Limited*, ITA E137 of 2024, decided 23 October 2025 | The entire commercial model rests on this holding. Should be confirmed by counsel and the judgment attached as an annex |
| V-03 | 9.13 use case to requirement traceability | Not verified mapping by mapping in this review |

## Presentational

- Versions 1.0, 2.0 and 2.1 all carry the date 31 August 2026, with 2.1 superseding 2.0 on the
  same day. Cosmetic, but it weakens the revision history in a document being signed.
- RSK-12 refers to "change control per Section 19". Section 19 is Risks; change control is
  Section 21. Section 6 cites it correctly.
- Sendy appears as a market-failure comparator in 4.2 and as the tax precedent in 14.1 without
  the document noting these are the same company. A reader may take it as coincidence.
- 5.8 implies approximately 1.2M per month of engineering during the nine-month build against
  780,000 per month for 3 FTE at steady state in 5.5. Probably a larger build team, but it is
  unexplained.
- OPEN-09 contradicts itself within a single row: the issue text requires the name to be settled
  "before any buyer-facing build begins", the Required-by column says Phase 7. Buyer-facing
  build begins in Phase 1. BR-ORD-01 has also already fixed the `MER-` order prefix, making a
  later rename a historical-data problem rather than a configuration change.

---

## Recommended order of work

Items 1 to 4 are arithmetic and presentation, and can be closed in a day. Items 5 to 7 are
structural and change what gets built.

| # | Action | Section | Finding |
|---|---|---|---|
| 1 | Rebuild the unit economics table | 5.3 | C-01 |
| 2 | Remove or substantiate the Month 22 breakeven claim | 1.3 | C-02 |
| 3 | Restate the funding ask against projected burn, in months of runway | 5.8 | C-03 |
| 4 | Re-derive OBJ-02 and OBJ-03 from the model's Year 1 exit run rate | 3 | C-04 |
| 5 | Add the fund-holding question as OPEN-10 and refer it to counsel with DEP-02 | 5.9, 14.3 | M-01 |
| 6 | Move the return window and holdback release into Phase 5, hardening before launch | 18 | M-04 |
| 7 | Re-baseline MoSCoW so RSK-12's mitigation has something to bite on | 10 | M-08 |

A corrected Section 5 with the arithmetic rebuilt and the working shown is in
`brd-v2.1-section-5-corrected.md`.
