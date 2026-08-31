---
title: "Project Meridian — Section 5 Rebuilt"
subtitle: "Corrected commercial model and unit economics, with working shown"
date: "31 August 2026"
lang: en-GB
---

## How to use this document

This replaces Sections 5.3, 5.4 and 5.7 of BRD v2.1 and adds the derivations the original
omitted. It corrects the arithmetic defect recorded as C-01 in `brd-v2.1-review.md` and
propagates the correction through the cohort model, the projection and the funding requirement.

Sections 5.1, 5.2, 5.5, 5.6, 5.9 and 5.10 of the original are unchanged and are not reproduced,
except where a stated assumption is made explicit below.

**Every figure here is reproducible.** The assumptions block below is the complete input set;
nothing in the tables uses a number that does not appear in it.

### Assumptions

| Input | Value | Source |
|---|---|---|
| Commission rate, mid-margin band | 9% of item value | 5.2 |
| Order processing fee | KES 40 per order, **borne by the vendor** | 5.1 REV-02, incidence newly stated |
| Delivery charged to buyer | KES 300 (pickup point) | 5.3 original |
| Carrier cost | KES 260 | 5.3 original |
| Payment aggregator fee | 2.9% of total buyer payment | 5.5 |
| SMS | 4 messages at KES 1.00 | 5.5 |
| Disbursement, apportioned per order | KES 12 | 5.3 original |
| Return rate | 4% of orders | 5.3 original |
| Reverse logistics cost per return | KES 250 | **Newly stated** — implicit in the original provision |
| Blended VAT leakage | 3.2% of GMV | 5.6, ASM-02 — see caveat below |
| Fixed operating cost, year 1 | KES 1,770,000 per month | 5.5 |

**Caveat on VAT leakage.** The 3.2% figure is carried forward from the original so the
correction is isolated to the arithmetic. It remains underived and is the largest single risk
in the model. Section 5.7b below models the alternatives.

---

## 5.3 Unit Economics — Single Order (corrected)

Representative order in the mid-margin band. All amounts in KES.

| Line | Amount | Note |
|---|---|---|
| Item value (GMV) | 2,400 | Basket assumption, single vendor |
| Delivery charged to buyer | 300 | Pickup-point rate |
| **Total buyer payment** | **2,700** | |
| | | |
| **Platform revenue lines** | | |
| Commission at 9% of item value | 216 | REV-01 |
| Order processing fee, borne by vendor | 40 | REV-02 |
| Delivery collected from buyer | 300 | Pass-through, offset by carrier cost below |
| **Total revenue** | **556** | |
| | | |
| **Platform cost lines** | | |
| Aggregator fee at 2.9% of 2,700 | (78) | |
| Carrier cost | (260) | Against 300 collected: net delivery margin 40 |
| SMS, 4 per order | (4) | |
| Disbursement, apportioned | (12) | Settlement batch cost divided by orders |
| **Total cost** | **(354)** | |
| | | |
| **Contribution before returns** | **202** | |
| Return provision: 4% x (202 + 250 reverse logistics) | (18) | |
| **Net contribution per order** | **184** | **7.7% of GMV; 6.8% of buyer payment** |

### What changed and why

The original table deducted the 260 carrier cost while omitting the 300 delivery revenue from
"gross platform revenue", then added a 40 "delivery margin retained" on top — counting the
delivery shortfall once and the delivery margin again. It also reported a contribution of 142
where its own stated lines summed to -58.

The corrected figure is **184**, against 124 in the original. The error understated
contribution by 48%.

**Independent check by cash movement.** The platform collects 2,700 from the buyer. It pays the
vendor 2,400 less 216 commission less 40 fee = 2,144. It pays the carrier 260, the aggregator
78, SMS 4 and disbursement 12. Remaining: 2,700 - 2,144 - 260 - 78 - 4 - 12 = **202** before the
return provision. This confirms the table.

### Sensitivity

| Lever | Net contribution | Change |
|---|---|---|
| Baseline, 9% commission | 184 | — |
| Commission 9% to 12% | 253 | +38% |
| Average basket 2,400 to 3,600 | 254 | +38% |
| Aggregator fee 2.9% to 2.2% (volume tier) | 202 | +10% |
| Pickup point share 60% to 85% | not computable | see below |

Commission and basket size are now of equal weight, at roughly +38% each. In the original,
basket size dominated at +87% against commission at +58% — an artefact of the arithmetic error,
which suppressed the commission lever by understating the base it multiplies.

This matters for strategy. The original conclusion was that basket size is "the strongest
lever, which reinforces the case for a category with higher average order value". On the
corrected figures, commission rate is equally strong, so the case for a differentiated vertical
rests on **both** a higher basket and a defensible commission band, not principally on basket
size. Section 5.7b shows commission is the lever that actually reaches breakeven.

**The pickup-point lever cannot be recomputed.** The original claimed +27% from raising pickup
share from 60% to 85%, but the representative order in 5.3 is already a pickup-point order and
the document nowhere states the door-delivery rate or its carrier cost. The lever is real —
door delivery is more expensive and less reliably recovered — but it cannot be quantified until
a two-mode rate card exists. Recorded as a gap against BR-FUL-08.

---

## 5.4 Unit Economics — Vendor Cohort (corrected)

| Metric | Value | Basis |
|---|---|---|
| Vendor acquisition cost | KES 2,800 | Unchanged |
| Orders per active vendor per month | 14 | Unchanged |
| Average vendor lifespan | 19 months | Assumes 55% six-month retention, OBJ-11 |
| Lifetime contribution per vendor | **KES 48,944** | 14 x 19 x 184 |
| Vendor LTV:CAC | **17.5:1** | Was 11.8:1 |

At 35% six-month retention, lifespan falls to approximately 11 months, lifetime contribution to
KES 28,336, and LTV:CAC to **10.1:1** — still viable.

**Gap carried forward from the original.** Buyer acquisition cost does not appear in this model
at all, although OBJ-10 sets a ceiling of KES 450 per acquired active buyer. A vendor-only LTV
model overstates the economics of a two-sided marketplace. See m-10 in the review: at OBJ-10's
ceiling, the marketing budget in 5.5 is roughly three times short of the buyer volume the
projection requires. Closing that gap will either raise the fixed cost line or lower the order
volumes below.

---

## 5.7 Three-Year Projection (corrected)

Order volumes, average order values and GMV are carried forward unchanged from the original.
Revenue and variable cost are recomputed from the corrected per-order model at each year's
average order value.

### Per-order economics by year

| | Year 1 | Year 2 | Year 3 |
|---|---|---|---|
| Average order value | 2,400 | 2,750 | 3,100 |
| Total buyer payment | 2,700 | 3,050 | 3,400 |
| Commission at 9% | 216.0 | 247.5 | 279.0 |
| Processing fee | 40.0 | 40.0 | 40.0 |
| Aggregator at 2.9% | (78.3) | (88.5) | (98.6) |
| Delivery margin | 40.0 | 40.0 | 40.0 |
| SMS and disbursement | (16.0) | (16.0) | (16.0) |
| Return provision | (18.1) | (18.9) | (19.8) |
| **Net contribution per order** | **183.6** | **204.1** | **224.6** |

### Projection

| | Year 1 | Year 2 | Year 3 |
|---|---|---|---|
| Active vendors, year end | 250 | 780 | 1,850 |
| Orders per month, year end | 3,500 | 12,400 | 31,000 |
| Orders in year | 24,500 | 77,040 | 156,877 |
| Average order value (KES) | 2,400 | 2,750 | 3,100 |
| **Annual GMV (KES)** | **58,800,000** | **211,860,000** | **486,320,000** |
| Gross revenue (KES) | 6,272,000 | 22,149,000 | 50,043,763 |
| Variable costs (KES) | 1,773,016 | 6,422,979 | 14,805,424 |
| VAT leakage at 3.2% (KES) | 1,881,600 | 6,779,520 | 15,562,240 |
| **Contribution (KES)** | **2,617,384** | **8,946,501** | **19,676,099** |
| Fixed operating cost (KES) | 21,240,000 | 25,488,000 | 31,860,000 |
| **EBITDA (KES)** | **(18,622,616)** | **(16,541,499)** | **(12,183,901)** |

Cumulative three-year EBITDA: **(47,348,016)**.

### What changed and why

Three defects in the original are corrected here.

**Gross revenue now ties to the stated model in every year.** The original's Year 1 figure of
6,272,000 tied exactly to 9% plus 40 per order. Years 2 and 3 did not — they were high by
1.16M and 5.88M respectively, implying an unexplained take-rate drift from 10.7% to 11.5%. If
REV-03 through REV-05 are intended to close that gap they must appear as their own lines; they
are excluded here, consistent with 5.1's statement that the model does not depend on them.

**Variable costs now derive from 5.3.** The original's variable cost line implied roughly 163
per order in Year 1, which reconciles to neither the 54 nor the 354 that the original 5.3 table
could be read as supporting. The corrected figure is 72.4 per order in Year 1, computed
directly from the per-order table above.

**The trend reverses.** The original showed EBITDA worsening every year (-20.8M, -23.8M,
-26.0M), which is the signature of a business whose variable costs exceed its gross margin. The
corrected model shows losses **narrowing** (-18.6M, -16.5M, -12.2M) as average order value
rises and fixed costs grow more slowly than volume. This is a materially different and more
defensible commercial story: the original implied a business that gets worse with scale, which
is not what its own unit economics describe.

**It still does not reach breakeven within three years at 9% commission.** That conclusion from
the original survives the correction, and Section 1.3's claim of breakeven at month 22 remains
unsupported by any version of this model.

---

## 5.7b Paths to Viability

The original identified three structural changes without quantifying them consistently. Each is
modelled here against Year 3.

| Scenario | Year 3 contribution | Year 3 EBITDA |
|---|---|---|
| Baseline, 9% blended commission | 19,676,099 | (12,183,901) |
| **12% blended commission**, viable only in a differentiated vertical | 33,682,078 | **1,822,078** |
| 9% commission, fixed cost held at Year 2 level | 19,676,099 | (5,811,901) |
| 12% commission and fixed cost held at Year 2 level | 33,682,078 | 8,194,078 |

**The commission band is the whole business case.** At 12% blended, Year 3 EBITDA turns positive
on the corrected model, without any assumption about REV-03 through REV-05 and without holding
headcount flat. Net contribution per order rises from 225 to 314.

This makes OPEN-01 — category selection — not merely the highest-priority open issue but the
determinant of whether the business exists. A 12% blended rate is not defensible in general
merchandise against the benchmarks in 4.3; it is defensible in the high-margin discretionary and
handmade bands identified in 5.2.

### VAT sensitivity

The blended leakage assumption is carried from the original and is undisclosed. Its range is
wide enough to invert the conclusion.

| Blended leakage | Year 3 contribution | Year 3 EBITDA |
|---|---|---|
| 3.2% — as assumed, mitigation (1) largely successful | 19,676,099 | (12,183,901) |
| 8.0% — partial mitigation, mixed vendor base | (3,667,261) | (35,527,261) |
| 14.0% — unmitigated principal classification | (32,846,461) | (64,706,461) |

At 14% — the rate implied by 5.6's own worked example of 337 per order on a 2,700 order — the
platform loses money on every transaction and no commission rate available in this market
recovers it. **Contribution turns negative somewhere between 6% and 7% leakage.**

This is the number that decides whether the model is a business or not, and it is currently a
single figure with no derivation. It requires the vendor-mix assumption behind it to be stated,
and a tax opinion under DEP-02 before pricing is fixed.

---

## 5.8 Funding Requirement (corrected)

| Item | Amount (KES) |
|---|---|
| Build to launch — engineering, 9 months | 10,800,000 |
| Infrastructure and tooling during build | 1,100,000 |
| Legal, tax, licensing, ODPC registration | 850,000 |
| Design and brand | 620,000 |
| Contingency at 15% | 1,000,000 |
| **Build subtotal** | **14,370,000** |
| Working capital | 9,200,000 |
| **Total requirement** | **23,570,000** |

**What this actually funds.** Year 1 EBITDA of -18,622,616 is an average monthly burn of
1,551,885. Working capital of 9,200,000 therefore funds approximately **5.9 months** of
post-launch operation, not the twelve months stated in the original.

The original's description of the ask is not supportable in either version of the model.
Restated honestly:

| Statement | Value |
|---|---|
| Funds build to launch | 9 months |
| Plus post-launch runway | ~6 months |
| Capital required to reach the end of Year 1 | 14,370,000 + 18,622,616 = **32,992,616** |
| Capital required to reach the end of Year 3 at 9% commission | 14,370,000 + 47,348,016 = **61,718,016** |
| Capital required to reach the end of Year 3 at 12% commission | 14,370,000 + 25,547,029 = **39,917,029** |
| **Peak funding need at 12% commission** (trough at end of Year 2, before Year 3 turns positive) | **41,739,107** |

At 12% commission the business turns EBITDA-positive in Year 3 (+1,822,078), so the peak
capital requirement — the figure that actually matters to an investor — is approximately
**41.7M**, reached at the end of Year 2. At 9% there is no trough within the modelled period
because losses continue throughout.

The 23,570,000 ask is a **first-tranche** figure covering build and roughly six months of
trading, at the end of which the business requires a further raise. That is a normal structure
and defensible on its own terms, but it must be presented as such. Presenting it alongside
"projected monthly breakeven: Month 22" implies the ask reaches breakeven, which it does not
under any scenario modelled here.

---

## Consequential amendments required elsewhere

| Section | Amendment |
|---|---|
| 1.3 | Delete "Projected monthly breakeven: Month 22 post-launch". Restate the funding line as build plus approximately 6 months runway |
| 3 OBJ-02 | Restate from 15,000,000 to 8,400,000 monthly GMV, or raise the vendor or basket assumptions in 5.4 and 5.7 to support it |
| 3 OBJ-03 | Restate from 1,200,000. The Year 1 exit run rate is 3,500 x 256 = 896,000 gross monthly, before variable costs |
| 5.1 REV-02 | State that the order processing fee is borne by the vendor, and confirm the VAT treatment of it with DEP-02 |
| 5.6 | Add the vendor-mix derivation behind the 3.2% blended leakage figure, and the 5.7b sensitivity table |
| 22 | Add OPEN-10, platform-controlled account and CBK fund-holding exposure — see M-01 in the review |
