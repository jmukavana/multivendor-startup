---
title: "Project Meridian — Multi-Vendor E-Commerce Marketplace"
subtitle: "Business Requirements Document v2.1"
author: "Jamlech Mukavana"
date: "31 August 2026"
lang: en-GB
---



```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## Document Control

| Field | Value |
|---|---|
| Document title | Project Meridian — Multi-Vendor Marketplace Business Requirements Document |
| Product name | **Not yet decided.** "Meridian" is a working codename with no brand significance. See OPEN-09. |
| Version | 2.2 |
| Date | 31 August 2026 |
| Author | Jamlech Mukavana |
| Status | Issued for client sign-off |
| Classification | Confidential — for named recipients only |
| Supersedes | v2.1, 31 August 2026 |

**Revision history**

| Version | Date | Author | Summary of change |
|---|---|---|---|
| 0.1 | — | J. Mukavana | Initial outline |
| 1.0 | 31 Aug 2026 | J. Mukavana | First complete draft incorporating market and regulatory research |
| 2.0 | 31 Aug 2026 | J. Mukavana | Added detailed use cases (Section 9), acceptance criteria (Section 11), and commercial model with unit economics (Section 5). Restructured for client sign-off. |
| 2.1 | 31 Aug 2026 | J. Mukavana | Product name deferred; document now uses the working codename "Meridian" throughout. Brand selection recorded as OPEN-09. Order reference prefix set to MER- pending naming. |
| 2.2 | 31 Aug 2026 | J. Mukavana | Corrected the unit economics in 5.3 and propagated the correction through 5.4, 5.7 and 5.8. Removed the unsupported breakeven claim in 1.3. Restated OBJ-02 and OBJ-03 against the model. Stated the incidence of REV-02. Added VAT sensitivity to 5.6 and OPEN-10 (platform-controlled account). Added BR-STL-12 (settlement idempotency) and AC-STL-07. Resequenced the roadmap in 18 so returns precede settlement and hardening precedes launch, with 11.11 renumbered to match and OPEN-11 raised for re-estimation. Re-baselined MoSCoW in 10 against a stated definition of launch, added per-module delivery phases, and moved 10 requirements from Must to Should. Stated in 14.1 that Section 10 assumes principal VAT classification and forecloses mitigation (2). Moved OPEN-06 to Phase 0 exit. Reconciled order creation timing across BR-ORD-01, UC-03 and 17.2. Added buyer-initiated cancellation (BR-ORD-13 to 16), platform integrity and abuse controls (new 10.16, BR-SEC-01 to 08), and the erasure-against-retention reconciliation (REG-21 to 24, DAT-06), each with acceptance criteria; RSK-07, RSK-08 and RSK-11 now trace to requirements. Restructured 15.1 by module with 16 entities added, and added DAT-07, DAT-08 and AC-STL-08 for versioned configuration under COM-05. See `brd-v2.1-review.md`. |

**Purpose of this document**

This document defines what the Meridian platform must do, why, and how its delivery will be judged complete. It is the controlling reference for scope. Anything not stated here is out of scope until introduced through the change control process in Section 21.

**How to read this document**

| If you are | Read first |
|---|---|
| Business sponsor or investor | Sections 1, 3, 4, 5, 19 |
| Product owner | Sections 6, 9, 10, 11, 22 |
| Engineering lead | Sections 10, 11, 12, 13, 15 |
| Finance | Sections 5, 10.8, 10.9, 14, 16 |
| Legal and tax advisor | Section 14 in full, plus 5.6 |

**Sign-off**

By signing below, each party confirms that the requirements in Sections 9 through 16 represent an accurate and complete statement of the agreed scope, and that the open issues in Section 22 are understood and accepted as outstanding at the point of sign-off.

| Role | Name | Signature | Date |
|---|---|---|---|
| Business Sponsor | | | |
| Product Owner | | | |
| Technical Lead | | | |
| Legal / Tax Advisor | | | |
| Finance | | | |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 1. Executive Summary

Meridian is a multi-vendor e-commerce marketplace for the Kenyan market. Independent vendors list products, receive orders, fulfil them through integrated logistics partners, and are settled net of a platform commission. Buyers get a single cart spanning multiple vendors, M-Pesa-first checkout, and delivery to either a physical address or a pickup point.

The platform is to be delivered as a modular monolith on Spring Boot with Spring Modulith, a Next.js buyer storefront, and deployment to Google Kubernetes Engine.

### 1.1 The Proposition

For vendors, Meridian converts a social-media following into a transacting business: payment guarantee, delivery integration, and tax-compliant invoicing without the vendor becoming a logistics or compliance operator. For buyers, it removes the counterparty risk inherent in buying from a small trader on Instagram or WhatsApp.

### 1.2 Findings That Shape This Document

Three research findings materially constrain the design and should be understood before reading the requirements.

**The platform will likely be treated as the principal seller for VAT purposes**, not as an agent earning commission. A 2025 Kenyan High Court decision holds that platforms exercising transactional control are liable for VAT on the full customer payment. This changes pricing, invoicing, and unit economics across the whole product. Section 5.6 models the impact; Section 14.1 states the requirement.

**The platform will not hold customer funds directly.** Collection and disbursement run through a Central Bank of Kenya licensed payment aggregator, avoiding Payment Service Provider licensing obligations. This is a hard architectural constraint, not a preference. See Section 14.3.

**Horizontal general-merchandise marketplaces have repeatedly failed in this market.** The commercial strategy must be vertical or otherwise differentiated. Section 4.2 sets out the evidence; Section 22 records category selection as the highest-priority open decision.

### 1.3 Investment Summary

| Item | Value |
|---|---|
| Development to launch (Phases 0–9) | 9 months — understated, see Section 18 and OPEN-11 |
| Estimated build cost to launch | KES 14.4M (Section 5.8) |
| Working capital, funding approximately 6 months post-launch | KES 9.2M (Section 5.8) |
| **Total first-tranche funding requirement** | **KES 23.6M** |
| Projected Year 3 GMV | KES 486M |
| Projected Year 3 EBITDA at 9% commission | (KES 12.2M) |
| Projected Year 3 EBITDA at 12% commission | KES 1.8M |
| Peak funding need to breakeven at 12% commission | KES 41.7M |

**The requested amount is a first tranche, not the capital required to reach breakeven.** It funds the nine-month build and approximately six months of post-launch trading, at the end of which a further raise is required. The model does not reach breakeven at a 9% blended commission on any horizon shown; at 12%, achievable only in a differentiated vertical, it turns EBITDA-positive in Year 3 against a peak capital requirement of approximately KES 41.7M. Section 5.7b sets out the paths and Section 5.8 the funding profile.

All figures in Section 5 are modelled on stated assumptions and are sensitive to the open issues in Section 22, principally category selection and VAT classification. They are planning estimates, not commitments.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 2. Business Context and Problem Statement

### 2.1 Context

Kenyan small and medium traders sell predominantly through social channels — Instagram, TikTok, WhatsApp and Facebook. These channels give reach but no transaction infrastructure: no order management, no payment guarantee, no delivery integration, no dispute mechanism, and no record adequate for tax purposes. Buyers on these channels carry the full counterparty risk, and vendors carry the full cost of building trust from scratch with every new customer.

The established marketplaces solve this but impose their own constraints: category commissions reaching the high twenties, mandatory logistics contributions, and onboarding oriented toward higher-volume sellers.

### 2.2 Problem Statement

Independent Kenyan vendors lack an affordable, trustworthy channel combining payment guarantee, delivery, and tax-compliant invoicing without requiring them to operate logistics or compliance functions themselves. Buyers lack a trusted way to transact with small vendors outside the incumbent platforms.

### 2.3 Consequences of the Status Quo

| Party | Consequence |
|---|---|
| Vendor | Lost sales from buyers unwilling to pay a stranger in advance; time lost to manual order and delivery coordination; no transaction record for credit or tax purposes |
| Buyer | Payment risk with no recourse; no delivery visibility; no reliable basis to judge vendor reliability |
| Platform opportunity | The transaction layer between the two is unserved for this segment |

### 2.4 Opportunity Statement

A marketplace that removes counterparty risk on both sides, integrates with the payment and delivery rails Kenyan traders already use, and handles tax invoicing automatically, can capture vendors currently stranded between social selling and the incumbents.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 3. Business Objectives and Success Metrics

| Ref | Objective | Metric | Target (12 months post-launch) | Measured by |
|---|---|---|---|---|
| OBJ-01 | Establish a viable vendor base | Active vendors (≥1 order in 30 days) | 250 | RPT-06 |
| OBJ-02 | Achieve transaction volume | Monthly GMV | KES 8,400,000 | RPT-05 |
| OBJ-03 | Generate platform revenue | Monthly gross commission and fee revenue | KES 896,000 | RPT-05 |
| OBJ-04 | Build buyer trust | Order completion rate (paid to delivered) | ≥ 92% | RPT-05 |
| OBJ-05 | Build buyer retention | Repeat purchase rate within 90 days | ≥ 30% | RPT-05 |
| OBJ-06 | Maintain financial integrity | Unreconciled payment transactions at month end | 0 | RPT-01 |
| OBJ-07 | Maintain regulatory standing | Tax invoices successfully transmitted | 100% of confirmed orders | RPT-04 |
| OBJ-08 | Control dispute cost | Orders escalating to dispute | ≤ 2% | RPT-07 |
| OBJ-09 | Vendor satisfaction | Vendors settled on schedule without manual intervention | ≥ 98% | RPT-02 |
| OBJ-10 | Control acquisition cost | Blended cost per acquired active buyer | ≤ KES 450 | RPT-05 |
| OBJ-11 | Vendor retention | Vendors active in month 6 who were active in month 1 | ≥ 55% | RPT-06 |

OBJ-02 and OBJ-03 are set to the Year 1 exit run rate projected in Section 5.7 (250 active vendors at 14 orders per vendor per month, average order value KES 2,400). They are the model's output, not an independent ambition; raising them requires raising the vendor, basket or frequency assumptions in Section 5.4 first.

Objectives are reviewed monthly. Failure against OBJ-01, OBJ-02 or OBJ-05 for two consecutive quarters triggers a strategy review under Section 21.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 4. Market Analysis

### 4.1 Competitive Landscape

| Player | Position | Relevance to Meridian |
|---|---|---|
| Jumia Kenya | Market leader; largest e-commerce retailer in Kenya by revenue | Sets the commission ceiling and the buyer expectation for delivery reliability |
| Kilimall | Established 2014; over 10,000 vendors, roughly half from mainland China | Competes on price and breadth of imported goods |
| Jiji | Classifieds; no transaction handling | Competes for vendor attention, not for transactions |
| Carrefour and formal retail | Growing online grocery and general merchandise | Competes for buyer spend in overlapping categories |
| Social commerce (Instagram, TikTok, WhatsApp) | Largest informal channel | The primary source of vendor supply for Meridian |

### 4.2 Failure Evidence

This market has a substantial graveyard and the pattern is consistent.

- **Sky.Garden** charged a flat 8% on completed trades and raised approximately USD 5.2 million but never reached profitability. It wound down in 2022, was acquired by Lipa Later for KES 250 million in December 2023, and that acquisition was itself cited as contributing to the acquirer's instability.
- **Lipa Later** was placed under administration in March 2025.
- **Masoko**, operated by Safaricom with direct access to M-Pesa and nationwide retail distribution, was deprioritised and its third-party vendors discontinued.
- **Sendy**, a logistics platform, has ceased operations.

**Implication.** The horizontal general-merchandise model is not viable at the capital available. The business case depends on a category where commission above 10% is defensible and where incumbent logistics is a poor fit. Recorded as OPEN-01.

### 4.3 Commission Benchmarks

Incumbent pricing establishes the ceiling. Jumia Kenya charges category-based commission in the range of roughly 2–13%, quoted inclusive of 16% VAT, plus a shipping cost contribution of approximately KES 160–2,500 depending on package size and delivery zone, plus order processing fees. Representative rates include mobile phones at roughly 6%, televisions at roughly 8%, fashion at 13%, and grocery categories reaching substantially higher.

### 4.4 Logistics Landscape

Third-party capacity exists and should be integrated rather than replicated. Relevant providers include Pickup Mtaani (agent pickup-point network, 50,000+ sellers), Fargo Courier, G4S, Boxleo, Codirect and Motorspeed, most supporting cash-on-delivery collection. Indicative delivery cost is KES 200–500 within Nairobi and KES 300–1,000 intercity.

Pickup-point delivery is strategically important because Kenya's informal addressing system makes door-to-door delivery unreliable and expensive. It must be a first-class delivery option, not a fallback.

### 4.5 Target Segments

| Segment | Description | Priority |
|---|---|---|
| Social-native vendors | Traders operating primarily on Instagram and TikTok with an established following but no transaction infrastructure | Primary |
| Physical retailers seeking online reach | Small shop owners with stock but no digital channel | Secondary |
| Urban mobile buyers aged 22–40 | Smartphone-first, M-Pesa-native, price sensitive, delivery-reliability sensitive | Primary buyer segment |
| Diaspora purchasers buying for family in Kenya | Pay from abroad, deliver locally | Deferred — requires cross-border scope |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 5. Commercial Model and Unit Economics

### 5.1 Revenue Streams

| Ref | Stream | Description | Phase | Year 3 share of revenue |
|---|---|---|---|---|
| REV-01 | Category commission | Percentage of item value, varying by category | 1 | 74% |
| REV-02 | Order processing fee | Fixed fee per order covering payment and platform cost, **borne by the vendor and deducted at settlement** | 1 | 12% |
| REV-03 | Delivery margin | Difference between buyer-charged and carrier-billed delivery | 3 | 6% |
| REV-04 | Promoted listings | Vendors pay for placement in search and category pages | 5 | 6% |
| REV-05 | Vendor subscription tiers | Enhanced tooling and reduced commission at a monthly fee | 6 | 2% |

Commission is the business. Everything else is margin improvement, and the model does not depend on any of REV-03 through REV-05 reaching target.

The incidence of REV-02 is stated because it changes both the vendor's effective rate — 10.7% rather than 9% on the representative order in 5.3 — and the VAT base, since a fee charged to the buyer would form part of the taxable supply. The treatment must be confirmed under DEP-02.

### 5.2 Commission Structure

Commission must be configurable by category, by vendor, and by product-level override, with a documented precedence order: product override, then vendor rate, then category rate, then platform default. The system must support minimum commission floors, promotional commission holidays with expiry, and full auditability of which rate applied to any historical order.

**Indicative launch schedule** (to be finalised once OPEN-01 closes):

| Category band | Commission | Rationale |
|---|---|---|
| High-margin discretionary (fashion accessories, beauty, home decor) | 12% | Below Jumia fashion at 13%, sustainable on vendor margin |
| Mid-margin general (home goods, kitchenware, bags) | 9% | Competitive against incumbents |
| Low-margin electronics and phones | 5% | Thin vendor margin; matched to incumbent rates |
| Handmade and artisan | 12% | Low price sensitivity, high differentiation |
| Platform default (uncategorised) | 10% | — |

Order processing fee at launch: KES 40 per sub-order, capped at KES 120 per parent order.

### 5.3 Unit Economics — Single Order

Modelled on a representative order in the mid-margin band. All amounts in KES. Every figure below derives from the assumptions in 5.2 and 5.5; no input is used that is not stated in one of them.

| Line | Amount | Note |
|---|---|---|
| Item value (GMV) | 2,400 | Basket assumption, single vendor |
| Delivery charged to buyer | 300 | Pickup-point rate |
| **Total buyer payment** | **2,700** | |
| | | |
| **Revenue** | | |
| Commission at 9% of item value | 216 | REV-01 |
| Order processing fee, borne by vendor | 40 | REV-02 |
| Delivery collected from buyer | 300 | Offset by carrier cost below |
| **Total revenue** | **556** | |
| | | |
| **Cost** | | |
| Payment aggregator fee at 2.9% of 2,700 | (78) | Section 5.5 |
| Carrier cost | (260) | Against 300 collected: net delivery margin 40 |
| SMS notifications, 4 per order | (4) | |
| Payment disbursement fee, apportioned | (12) | Settlement batch cost divided by orders |
| **Total cost** | **(354)** | |
| | | |
| **Contribution before returns** | **202** | |
| Return provision: 4% × (202 + 250 reverse logistics) | (18) | |
| **Net contribution per order** | **184** | 7.7% of GMV; 6.8% of buyer payment |

**Check by cash movement.** The platform collects 2,700 from the buyer. It pays the vendor 2,400 less 216 commission less 40 processing fee, being 2,144. It pays the carrier 260, the aggregator 78, SMS 4 and disbursement 12. What remains is 2,700 − 2,144 − 260 − 78 − 4 − 12 = 202 before the return provision, which confirms the table.

**Read this carefully.** Net contribution is 7.7% of GMV against a headline 9% commission. The gap is the payment aggregator fee and the return provision; delivery contributes a positive 40 rather than a drag, because the pickup-point rate is recovered in full from the buyer. Three levers move the figure materially:

| Lever | Net contribution | Change |
|---|---|---|
| Baseline, 9% commission | 184 | — |
| Commission 9% → 12% | 253 | +38% |
| Average basket 2,400 → 3,600 | 254 | +38% |
| Aggregator fee 2.9% → 2.2% (volume tier) | 202 | +10% |

Commission rate and basket size carry equal weight. The commercial case for a differentiated vertical therefore rests on both a higher average order value **and** a defensible commission band, not principally on basket size. Section 5.7b shows that commission is the lever that reaches breakeven.

The share of orders delivered to a pickup point rather than to the door is a real fourth lever — door delivery costs more and is recovered less reliably — but it cannot be quantified until a two-mode rate card exists under BR-FUL-08. It is excluded from this model rather than estimated.

### 5.4 Unit Economics — Vendor Cohort

| Metric | Assumption | Basis |
|---|---|---|
| Vendor acquisition cost | KES 2,800 | Field agent time, onboarding support, verification |
| Orders per active vendor per month | 14 | Conservative for a social-native vendor at launch scale |
| Average vendor lifespan | 19 months | Assumes 55% six-month retention (OBJ-11) |
| Lifetime contribution per vendor | KES 48,944 | 14 × 19 × 184 |
| Vendor LTV:CAC | 17.5:1 | Healthy; sensitive to retention assumption |

If six-month retention falls to 35%, average lifespan drops to roughly 11 months, lifetime contribution to KES 28,336, and LTV:CAC to 10.1:1 — still viable, but it changes the acquisition budget materially. Vendor retention is therefore a tracked objective (OBJ-11), not an assumption.

**This model covers vendor acquisition only.** Buyer acquisition cost does not appear in it, although OBJ-10 sets a ceiling of KES 450 per acquired active buyer. A vendor-only lifetime model overstates the economics of a two-sided marketplace, and the marketing line in 5.5 is not sized to the buyer volume the projection in 5.7 requires: at the OBJ-10 ceiling, KES 350,000 per month acquires approximately 780 buyers, against a requirement in the order of 2,000 or more per month to sustain 3,500 monthly orders at a 30% ninety-day repeat rate. Closing that gap will either raise the fixed cost line in 5.5 or lower the volumes in 5.7. It is not resolved in this version and is the largest known omission in the commercial model after OPEN-02.

### 5.5 Cost Structure

**Variable cost per transaction**

| Cost | Rate | Notes |
|---|---|---|
| Payment aggregator collection | 2.5–3.5% | Negotiable at volume; 2.9% modelled |
| Payment disbursement to vendor | KES 15–35 per transfer | Batched by settlement cycle to reduce per-order cost |
| Carrier delivery | KES 200–1,000 | Zone dependent |
| SMS | KES 0.80–1.20 per message | Volume dependent |

**Fixed monthly operating cost at steady state**

| Cost | Monthly (KES) |
|---|---|
| Cloud infrastructure (GKE, database, storage, egress) | 145,000 |
| Engineering (3 FTE loaded) | 780,000 |
| Operations and support (2 FTE) | 210,000 |
| Vendor acquisition (1 field lead) | 130,000 |
| Marketing | 350,000 |
| Compliance, audit, legal retainer | 95,000 |
| Third-party services and tooling | 60,000 |
| **Total** | **1,770,000** |

### 5.6 VAT Sensitivity — The Critical Variable

Section 14.1 explains why the platform will likely be classified as principal rather than agent for VAT purposes. The commercial consequence is severe enough to model both cases explicitly.

| | Agent classification | Principal classification |
|---|---|---|
| VAT charged on | Commission only (216) | Full buyer payment (2,700) |
| VAT amount at 16% | 35 | 372 |
| Recoverable as input VAT | — | Vendor-side VAT recoverable only where the vendor is VAT-registered |
| Effect if absorbed by platform | Net contribution 124 | Net contribution materially negative |
| Effect if passed to buyer | Prices unchanged | Prices rise ~16%, harming conversion against incumbents |
| Effect if passed to vendor | Commission unchanged | Effective vendor cost rises sharply, harming vendor acquisition |

**This cannot be resolved inside the product.** It is a structuring question requiring a tax opinion before pricing is fixed. Three mitigations exist and are not mutually exclusive:

1. **Vendor VAT registration requirement.** Onboarding only VAT-registered vendors makes the input VAT recoverable through the chain, substantially reducing the leakage. This shrinks the addressable vendor pool and conflicts with the social-native segment in Section 4.5.
2. **Structural agency.** Restrict the platform's transactional control — vendors set price and arrange fulfilment, the platform does not collect — to support agent classification. This removes escrow, which is the core buyer-trust proposition.
3. **Price for principal.** Accept the classification and set commission high enough to absorb it, accepting a higher headline rate than incumbents.

The projections in Section 5.7 assume mitigation (1) with a mixed vendor base and a blended effective VAT leakage of 3.2% of GMV. This assumption is recorded as ASM-02 and is the single largest risk to the model (RSK-01).

**The 3.2% figure is not yet derived.** It implies that roughly 77% of the exposure shown above is recovered through the chain, against a worked example of 337 per order — approximately 14% of GMV — if none of it is. The vendor mix that produces 3.2%, and the VAT-registered share it assumes, must be stated before this document is relied upon for pricing. Until then the figure is an input to be tested, not a finding.

**Sensitivity of Year 3 to blended leakage**, all other assumptions as in 5.7:

| Blended leakage | Year 3 contribution (KES) | Year 3 EBITDA (KES) |
|---|---|---|
| 3.2% — mitigation (1) largely successful | 19,676,099 | (12,183,901) |
| 8.0% — partial mitigation, mixed vendor base | (3,667,261) | (35,527,261) |
| 14.0% — unmitigated principal classification | (32,846,461) | (64,706,461) |

Contribution turns negative between 6% and 7% leakage. At 14% the platform loses money on every transaction and no commission rate available in this market recovers it. This single input therefore decides whether the model describes a business, which is why REG-01 requires the classification to be formally determined by a tax advisor before pricing is finalised, and why DEP-02 is required at Phase 0 rather than later.

### 5.7 Three-Year Projection

Assumes launch at month 0 following a 9-month build. Vendor and order growth modelled on gradual field acquisition, not paid-media scale. Revenue and variable cost are computed from the per-order model in 5.3, restated at each year's average order value. Revenue comprises commission and processing fee only; REV-03 through REV-05 are excluded, consistent with 5.1.

**Per-order economics by year**

| | Year 1 | Year 2 | Year 3 |
|---|---|---|---|
| Average order value (KES) | 2,400 | 2,750 | 3,100 |
| Total buyer payment (KES) | 2,700 | 3,050 | 3,400 |
| Commission at 9% | 216.0 | 247.5 | 279.0 |
| Order processing fee | 40.0 | 40.0 | 40.0 |
| Payment aggregator at 2.9% | (78.3) | (88.5) | (98.6) |
| Delivery margin | 40.0 | 40.0 | 40.0 |
| SMS and disbursement | (16.0) | (16.0) | (16.0) |
| Return provision | (18.1) | (18.9) | (19.8) |
| **Net contribution per order** | **183.6** | **204.1** | **224.6** |

**Projection**

| | Year 1 | Year 2 | Year 3 |
|---|---|---|---|
| Active vendors (year end) | 250 | 780 | 1,850 |
| Orders per month (year end) | 3,500 | 12,400 | 31,000 |
| Orders in year | 24,500 | 77,040 | 156,877 |
| Average order value (KES) | 2,400 | 2,750 | 3,100 |
| **Annual GMV (KES)** | **58,800,000** | **211,860,000** | **486,320,000** |
| Gross revenue (KES) | 6,272,000 | 22,149,000 | 50,043,763 |
| Variable costs (KES) | 1,773,016 | 6,422,979 | 14,805,424 |
| VAT leakage at 3.2% of GMV (KES) | 1,881,600 | 6,779,520 | 15,562,240 |
| **Contribution (KES)** | **2,617,384** | **8,946,501** | **19,676,099** |
| Fixed operating cost (KES) | 21,240,000 | 25,488,000 | 31,860,000 |
| **EBITDA (KES)** | **(18,622,616)** | **(16,541,499)** | **(12,183,901)** |

Cumulative three-year EBITDA is (47,348,016).

**The projection does not reach profitability within three years at a 9% blended commission.** This is stated plainly because it is the honest output of the model, and because every failed comparator in Section 4.2 failed for exactly this reason.

Losses do, however, narrow year on year, as average order value rises and fixed cost grows more slowly than volume. That is the shape a viable marketplace should show before it turns; a business whose losses widen with scale has a unit economics problem that growth cannot fix. Section 5.7b sets out what closes the remaining gap.

### 5.7b Paths to Viability

Each structural change is modelled against Year 3.

| Scenario | Year 3 contribution (KES) | Year 3 EBITDA (KES) |
|---|---|---|
| Baseline, 9% blended commission | 19,676,099 | (12,183,901) |
| **12% blended commission**, viable only in a differentiated vertical | 33,682,078 | **1,822,078** |
| 9% commission, fixed cost held at Year 2 level | 19,676,099 | (5,811,901) |
| 12% commission and fixed cost held at Year 2 level | 33,682,078 | 8,194,078 |

**The commission band is the business case.** At 12% blended, Year 3 turns EBITDA-positive without assuming any of REV-03 through REV-05 and without holding headcount flat; net contribution per order rises from 225 to 314. A 12% blended rate is not defensible in general merchandise against the benchmarks in 4.3. It is defensible in the high-margin discretionary and handmade bands in 5.2.

This makes OPEN-01 — category selection — not merely the highest-priority open issue but the determinant of whether the business exists. **Sign-off on scope should not be read as validation of the commercial case**, which remains contingent on OPEN-01 and OPEN-02 rather than on execution of the requirements in this document.

### 5.8 Funding Requirement

| Item | Amount (KES) |
|---|---|
| Build to launch — engineering, 9 months | 10,800,000 |
| Infrastructure and tooling during build | 1,100,000 |
| Legal, tax, licensing, ODPC registration | 850,000 |
| Design and brand | 620,000 |
| Contingency at 15% | 1,000,000 |
| **Build subtotal** | **14,370,000** |
| Working capital | 9,200,000 |
| **First tranche total** | **23,570,000** |

**What this funds.** Year 1 EBITDA of (18,622,616) is an average monthly burn of 1,551,885. Working capital of 9,200,000 therefore funds approximately **5.9 months** of post-launch operation. The first tranche covers the nine-month build and roughly six months of trading, at the end of which a further raise is required.

**Capital to breakeven**

| Measure | Amount (KES) |
|---|---|
| Capital to the end of Year 1 | 14,370,000 + 18,622,616 = 32,992,616 |
| Capital to the end of Year 3 at 9% commission | 14,370,000 + 47,348,016 = 61,718,016 |
| Capital to the end of Year 3 at 12% commission | 14,370,000 + 25,547,029 = 39,917,029 |
| **Peak funding need at 12% commission** | **41,739,107** |

At 12% commission the business turns EBITDA-positive in Year 3, so the peak capital requirement — the figure that governs the raise — is approximately KES 41.7M, reached at the end of Year 2. At 9% there is no trough within the modelled period, because losses continue throughout and no total capital figure can be stated.

### 5.9 Settlement Model

Funds are collected by the payment aggregator into a platform-controlled account. The platform calculates vendor entitlement net of commission, fees, refunds and holdback, and instructs disbursement on the settlement cycle. Settlement frequency is a business decision; the system must support configuration between daily and monthly.

A holdback percentage is retained against return and dispute risk and released after the return window closes. Holdback is working capital for the platform and a cash-flow cost to the vendor, so the rate is a commercial negotiation, not purely a risk decision. Indicative launch setting: 10% held for 14 days.

### 5.10 Pricing Governance

| Ref | Requirement |
|---|---|
| COM-01 | Commission rates shall be changeable by configuration without code deployment |
| COM-02 | A rate change shall never apply retrospectively to orders already placed |
| COM-03 | Vendors shall receive no less than 30 days notice of a commission increase |
| COM-04 | Every rate change shall be recorded with actor, timestamp, previous value and new value |
| COM-05 | The system shall be able to reproduce, for any historical order, the exact rate applied and the configuration version that produced it |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 6. Scope

### 6.1 In Scope

- Buyer-facing storefront (web, mobile-responsive / PWA)
- Vendor registration, verification, and self-service dashboard
- Product catalog with vendor-scoped ownership
- Inventory management with reservation
- Multi-vendor cart and checkout
- Order splitting into per-vendor sub-orders
- M-Pesa and card payment collection via a licensed aggregator
- Double-entry financial ledger
- Commission calculation, holdback, and vendor settlement
- Integration with third-party logistics providers
- Electronic tax invoice generation and transmission
- Returns, refunds, and dispute management
- Reviews and ratings
- Search and product discovery
- Administrative back-office
- Reporting for vendors, finance, and management

### 6.2 Out of Scope (Phase 1)

| Item | Rationale |
|---|---|
| Own logistics fleet or warehousing | Capital-intensive; third-party capacity is adequate |
| Buy-now-pay-later or any credit product | Requires separate licensing and underwriting capability |
| Vendor working-capital lending | As above |
| Native iOS/Android applications | PWA delivers mobile coverage faster |
| Cross-border sales or multi-currency | Adds tax and settlement complexity without near-term revenue |
| Vendor-managed storefront subdomains | Deferred pending demand |
| Auction or bidding mechanics | Not aligned with the target model |
| B2B / wholesale pricing tiers | Deferred to a later phase |

### 6.3 Assumptions

| Ref | Assumption | Impact if false |
|---|---|---|
| ASM-01 | A CBK-licensed aggregator will onboard the platform with sub-account or split-payment capability | Forces direct Daraja integration and a PSP licensing review — major schedule impact |
| ASM-02 | The platform will be classified as principal for VAT purposes | If agent classification is achievable, unit economics improve materially |
| ASM-03 | Vendors will accept settlement on a defined cycle rather than instant payout | Instant settlement materially increases fraud exposure and working capital need |
| ASM-04 | At least one logistics partner will provide an API for rates, dispatch, and tracking | Manual dispatch workflows required, increasing operating cost |
| ASM-05 | Target category will be confirmed before Phase 2 begins | Catalog schema and commission model cannot be finalised |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 7. Stakeholders

| Stakeholder | Interest | Involvement |
|---|---|---|
| Business Sponsor | Return on investment, strategic direction | Approves BRD, scope changes, budget |
| Product Owner | Feature prioritisation, backlog | Daily; owns acceptance |
| Vendors | Sales volume, timely settlement, low fees | Consulted during design; pilot participants |
| Buyers | Price, trust, delivery reliability | Researched; usability testing |
| Finance | Reconciliation, revenue recognition, settlement accuracy | Reviews ledger and settlement requirements |
| Legal / Tax Advisor | VAT position, licensing exposure, data protection | Must approve Section 14 |
| Logistics Partners | Order volume, integration quality | Commercial negotiation and technical integration |
| Payment Aggregator | Transaction volume, compliance posture | Commercial negotiation and technical integration |
| Engineering Team | Deliverability, technical debt | Estimation, delivery |
| Customer Support | Tooling adequacy, case volume | Reviews back-office requirements |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 8. Actors and Roles

| Actor | Description | Key permissions |
|---|---|---|
| Guest | Unauthenticated visitor | Browse, search, view products, add to cart |
| Buyer | Registered purchaser | Checkout, order history, returns, reviews, saved addresses |
| Vendor Owner | Primary account holder for a selling business | Full vendor account control, bank/M-Pesa payout details, staff management |
| Vendor Staff | Delegated vendor user | Product and order management only; no payout or banking access |
| Platform Admin | Operational staff | Vendor approval, dispute resolution, catalog moderation, order intervention |
| Finance Admin | Settlement and reconciliation staff | Ledger access, settlement runs, refund authorisation, reporting |
| Super Admin | System owner | All permissions including role assignment and configuration |
| System | Automated processes | Scheduled settlement, reconciliation, invoice transmission, notifications |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 9. Use Cases

Use cases describe the platform from the actor's point of view. Each traces to the functional requirements in Section 10 and is verified by the acceptance criteria in Section 11.

**Notation.** *Main flow* is the successful path. *Alternate flows* are valid variations. *Exception flows* are failures the system must handle. Every exception flow listed is a required behaviour, not an edge case to be deferred.

---

### UC-01 — Vendor Registers and Is Approved

| | |
|---|---|
| **Actor** | Prospective Vendor, Platform Admin |
| **Goal** | Become an approved vendor able to list products |
| **Preconditions** | Applicant has a national ID and a KRA PIN |
| **Traces to** | BR-VND-01 to BR-VND-09 |

**Main flow**

1. Applicant creates an account with a phone number and verifies it by one-time code.
2. Applicant completes the business profile: business name, registration number, KRA PIN, physical address, contact details.
3. Applicant uploads national ID, business registration certificate and KRA PIN certificate.
4. Applicant submits the application. State becomes Submitted.
5. System places the application in the admin approval queue and notifies the applicant of receipt.
6. Admin reviews the profile and documents. State becomes Under Review.
7. Admin approves. State becomes Approved.
8. System notifies the applicant and prompts for payout details.
9. Applicant enters M-Pesa number or bank account. System verifies ownership.
10. Applicant may now create products.

**Alternate flows**

- **A1 — Information requested.** At step 6 the admin requests additional information. State becomes Information Requested with a stated reason. Applicant amends and resubmits, returning to step 4.
- **A2 — Sole trader without business registration.** At step 3 the applicant supplies national ID and KRA PIN only. Application proceeds under a reduced verification tier with a lower initial settlement limit.

**Exception flows**

- **E1 — Rejection.** Admin rejects with a mandatory reason. State becomes Rejected. Applicant is notified with the reason and may reapply after 30 days.
- **E2 — Document unreadable.** System accepts the upload but the admin cannot verify it. Handled as A1.
- **E3 — Duplicate KRA PIN.** System detects the PIN already registered to an approved vendor and blocks submission with an explanatory message.
- **E4 — Payout verification fails.** At step 9 verification fails. Vendor may list products but no settlement is executed until verification succeeds.

**Postconditions** — Vendor is Approved with verified payout details, or is in a terminal Rejected state with the reason recorded.

---

### UC-02 — Vendor Lists a Product

| | |
|---|---|
| **Actor** | Vendor Owner or Vendor Staff |
| **Goal** | Publish a purchasable product |
| **Preconditions** | Vendor is Approved and not suspended |
| **Traces to** | BR-CAT-01 to BR-CAT-12, BR-INV-01 |

**Main flow**

1. Vendor selects Create Product.
2. Vendor enters title, description, category, price and stock quantity.
3. Vendor uploads at least one image.
4. Vendor optionally defines variants, each with independent price and stock.
5. Vendor submits. State becomes Pending Review.
6. Admin reviews against catalog policy and approves. State becomes Active.
7. Product becomes visible in search and category listings.

**Alternate flows**

- **A1 — Bulk upload.** Vendor uploads a spreadsheet. System validates each row and returns a per-row error report. Valid rows enter Pending Review; invalid rows are not created.
- **A2 — Save as draft.** Vendor saves incomplete. State remains Draft and the product is not visible.
- **A3 — Auto-approval.** Vendors above a configured trust threshold bypass step 6 and publish directly, subject to post-publication sampling.

**Exception flows**

- **E1 — Prohibited item.** System matches the listing against the prohibited-items list and blocks submission with the matched reason.
- **E2 — Admin rejection.** Admin rejects with a stated reason. State becomes Rejected. Vendor may amend and resubmit.
- **E3 — No image.** System blocks submission at step 5.
- **E4 — Vendor suspended mid-review.** Product remains Pending Review and does not publish on approval until the vendor is reinstated.

**Postconditions** — Product is Active and purchasable, or is in Draft, Pending Review or Rejected and is not purchasable.

---

### UC-03 — Buyer Places a Multi-Vendor Order

| | |
|---|---|
| **Actor** | Buyer |
| **Goal** | Purchase items from more than one vendor in a single payment |
| **Preconditions** | Cart contains Active products from at least two vendors |
| **Traces to** | BR-CHK-01 to BR-CHK-09, BR-ORD-01 to BR-ORD-04, BR-INV-02 |

**Main flow**

1. Buyer opens the cart. System displays items grouped by vendor.
2. Buyer selects a delivery method for each vendor group: door delivery or pickup point.
3. System calculates delivery cost per vendor group from origin, destination and package characteristics.
4. Buyer proceeds to checkout. System revalidates price and stock for every line.
5. System reserves stock for every line with a configured expiry.
6. System displays the full breakdown: items, delivery per group, taxes, total.
7. Buyer confirms and enters the M-Pesa number.
8. System creates the parent order and one sub-order per vendor, all in Pending Payment, with reference MER-XXXXXXXX, and initiates the payment prompt.
9. Buyer authorises on the handset.
10. System receives and verifies the payment callback. Payment state becomes Confirmed.
11. System deducts reserved stock, moves each sub-order to Confirmed, and emits the tax invoice.
12. System notifies each vendor of their sub-order and confirms the order to the buyer.

**Alternate flows**

- **A1 — Single vendor.** Steps proceed identically; one sub-order is generated.
- **A2 — Price changed since add-to-cart.** At step 4 system notifies the buyer of the change and requires explicit acceptance before proceeding.
- **A3 — Card payment.** At step 7 buyer selects card. Flow diverges to the aggregator's hosted card page and rejoins at step 10.
- **A4 — Amount exceeds M-Pesa limit.** At step 8 system detects the total exceeds the per-transaction ceiling and offers an alternative payment path rather than failing.

**Exception flows**

- **E1 — Stock unavailable at revalidation.** System notifies the buyer, removes or reduces the line, and recalculates before allowing progression.
- **E2 — Buyer does not authorise.** Prompt times out. Payment state becomes Timed Out, reserved stock is released, no order is created.
- **E3 — Callback not received.** System polls the provider for status after the configured window and resolves the payment to Confirmed or Failed. No order progresses on an unresolved payment.
- **E4 — Duplicate callback.** System processes idempotently on the provider transaction reference. No double credit, no duplicate order.
- **E5 — Vendor suspended between cart and checkout.** System blocks checkout, identifies the affected group, and offers to remove it.
- **E6 — Payment confirmed but sub-order generation fails.** Payment stands, incident is raised to the exception queue, and the order is completed by admin intervention. Funds are never retained without an order or a refund.

**Postconditions** — Parent order exists with sub-orders in Confirmed, stock is deducted, tax invoice is emitted, ledger entries are posted. Or no order exists and no funds are held.

---

### UC-04 — Vendor Fulfils a Sub-Order

| | |
|---|---|
| **Actor** | Vendor, Carrier |
| **Goal** | Get the goods to the buyer and become eligible for settlement |
| **Preconditions** | Sub-order is Confirmed |
| **Traces to** | BR-ORD-03, BR-ORD-05, BR-FUL-01 to BR-FUL-07 |

**Main flow**

1. Vendor is notified of a new sub-order.
2. Vendor accepts within the configured acceptance window. State becomes Accepted.
3. Vendor books dispatch. System requests a carrier collection and receives a tracking reference.
4. Vendor hands the parcel to the carrier. State becomes Dispatched.
5. System updates state from carrier tracking events. State becomes In Transit.
6. Carrier records delivery. State becomes Delivered.
7. System notifies the buyer and opens the return window.

**Alternate flows**

- **A1 — Pickup point.** At step 4 the parcel is delivered to the agent point. Buyer is notified on arrival and state becomes Delivered on collection, not on arrival.
- **A2 — Manual status update.** Where carrier integration is unavailable, the vendor updates state manually at steps 4 and 6.
- **A3 — Partial dispatch.** Vendor dispatches available lines and cancels the rest, triggering a partial refund per UC-06.

**Exception flows**

- **E1 — Not accepted in time.** System auto-cancels the sub-order, triggers a refund for that portion, and records the cancellation against vendor performance.
- **E2 — Vendor declines.** Same as E1 but recorded with the stated reason.
- **E3 — Carrier booking fails.** System retries, then surfaces the failure to the vendor with a manual dispatch option.
- **E4 — Delivery fails after multiple attempts.** Carrier returns the parcel. State becomes Returned. Buyer is refunded, commission is reversed, carrier cost is apportioned per the vendor agreement.
- **E5 — Tracking events stop.** After a configured silence period, system flags the sub-order to operations.

**Postconditions** — Sub-order is Delivered and within the return window, or is Cancelled or Returned with the corresponding refund executed.

---

### UC-05 — Platform Settles a Vendor

| | |
|---|---|
| **Actor** | System, Finance Admin |
| **Goal** | Pay a vendor the correct amount and record it |
| **Preconditions** | Vendor has delivered orders past the return window |
| **Traces to** | BR-STL-01 to BR-STL-12, BR-LDG-01 to BR-LDG-07 |

**Main flow**

1. Return window closes on eligible orders. System releases the associated holdback.
2. Settlement period reaches cut-off.
3. System calculates entitlement per vendor: gross sales less commission, less fees, less refunds, less retained holdback.
4. System generates the reconciliation report and the settlement pre-run report.
5. Finance reviews and approves the run.
6. System instructs disbursement through the payment provider for each vendor.
7. System records each disbursement outcome and posts the ledger entries.
8. System issues the itemised vendor statement and notifies each vendor.

**Alternate flows**

- **A1 — Negative entitlement.** Where refunds exceed sales, the balance carries forward as a debit against the next settlement rather than being disbursed.
- **A2 — Manual hold.** Finance places an individual vendor on hold with a reason. That vendor is excluded from the run; all others proceed.

**Exception flows**

- **E1 — Reconciliation variance.** Ledger and provider balances disagree. The run is blocked entirely and an incident is raised. No partial settlement is executed on an unreconciled position.
- **E2 — Disbursement fails.** System retries per policy, then escalates to finance. Entitlement remains payable and is not lost.
- **E3 — Payout details changed within cooling-off.** Vendor is excluded from the run and notified of the reason.
- **E4 — Provider unavailable at step 6.** Run is suspended and resumes from the last confirmed disbursement without re-paying any vendor already settled.

**Postconditions** — Every vendor in the run is either disbursed and recorded, or excluded with a recorded reason. The ledger reconciles.

---

### UC-06 — Buyer Returns an Item and Is Refunded

| | |
|---|---|
| **Actor** | Buyer, Vendor, Platform Admin |
| **Goal** | Return goods and recover payment |
| **Preconditions** | Sub-order is Delivered and within the return window |
| **Traces to** | BR-RTN-01 to BR-RTN-09, BR-PAY-11, BR-ORD-07 |

**Main flow**

1. Buyer raises a return, selecting a reason from the controlled list and attaching images.
2. System notifies the vendor and pauses any pending settlement on the affected order.
3. Vendor accepts the return.
4. Goods are returned per the agreed method.
5. Vendor confirms receipt.
6. System authorises the refund, reverses commission proportionally, and restores stock where the goods are resaleable.
7. System instructs the refund through the payment provider and issues a credit note.
8. System notifies the buyer.

**Alternate flows**

- **A1 — Partial return.** Only some lines are returned. Refund and commission reversal are calculated on those lines only; the remainder of the sub-order is unaffected.
- **A2 — Refund without return.** For low-value items where return shipping exceeds item value, admin authorises refund without physical return.

**Exception flows**

- **E1 — Vendor rejects.** Case escalates to the admin dispute queue with the full order, payment and communication history. Admin resolves in favour of either party with a mandatory reason.
- **E2 — Vendor does not respond.** After the configured window the case auto-escalates as E1.
- **E3 — Holdback insufficient.** Refund is executed to the buyer and the shortfall is recovered from future vendor settlements as a carried debit.
- **E4 — Refund instruction fails.** System retries and escalates to finance. The buyer's entitlement is never cancelled by a technical failure.
- **E5 — Original payment method unavailable.** Refund is issued to a verified alternative belonging to the buyer, with the substitution recorded.

**Postconditions** — Buyer is refunded the correct amount, commission is reversed, the ledger balances, and the case is closed with an audit trail.

---

### UC-07 — Admin Resolves a Dispute

| | |
|---|---|
| **Actor** | Platform Admin |
| **Goal** | Reach a binding resolution between buyer and vendor |
| **Preconditions** | A case exists in the dispute queue |
| **Traces to** | BR-RTN-06, BR-RTN-07, BR-ADM-04, BR-ADM-07 |

**Main flow**

1. Admin opens the case and reviews order history, payment record, delivery evidence, and messages between the parties.
2. Admin requests further evidence from either party if required.
3. Admin decides in favour of one party and records a mandatory reason.
4. System executes the consequence: refund, release of settlement, or dismissal.
5. System notifies both parties with the decision and reason.
6. System records the outcome against vendor performance metrics.

**Exception flows**

- **E1 — Evidence not supplied.** Admin decides on available evidence after the configured deadline.
- **E2 — Amount exceeds admin authority.** Case escalates to Finance Admin for authorisation before step 4.

**Postconditions** — Case is closed with a recorded decision, reason, actor and timestamp. The financial consequence is executed and reflected in the ledger.

---

### UC-08 — Buyer Discovers a Product

| | |
|---|---|
| **Actor** | Guest or Buyer |
| **Goal** | Find a product matching an intent |
| **Preconditions** | None |
| **Traces to** | BR-SCH-01 to BR-SCH-06 |

**Main flow**

1. Actor enters a search term or browses a category.
2. System returns matching Active products from non-suspended vendors, excluding out-of-stock items by default.
3. Actor refines by category, price range, vendor, rating or delivery availability.
4. Actor sorts by relevance, price, rating or recency.
5. Actor opens a product and views detail, images, vendor identity and reviews.
6. Actor adds to cart.

**Alternate flows**

- **A1 — Zero results.** System logs the term for merchandising review and offers related categories and corrected spellings.
- **A2 — Guest.** All steps available; registration is required only at checkout.

**Postconditions** — Actor has either added an item to cart or exited. Search terms returning nothing are logged.

---

### UC-09 — Admin Suspends a Vendor

| | |
|---|---|
| **Actor** | Platform Admin |
| **Goal** | Stop a vendor trading without harming buyers with orders in flight |
| **Preconditions** | Vendor is Approved |
| **Traces to** | BR-VND-10, BR-ORD-11, BR-STL-09 |

**Main flow**

1. Admin selects the vendor and initiates suspension with a mandatory reason.
2. System immediately delists all of that vendor's products from search and browse.
3. System blocks new checkouts containing that vendor's items.
4. System preserves all in-flight sub-orders and continues to require fulfilment.
5. System places the vendor's settlement on hold.
6. System notifies the vendor with the reason.

**Alternate flows**

- **A1 — Reinstatement.** Admin lifts the suspension. Products return to their prior state, not automatically to Active.

**Exception flows**

- **E1 — In-flight orders not fulfilled.** After the acceptance window those sub-orders auto-cancel and buyers are refunded, with the cost recovered from held settlement.

**Postconditions** — Vendor cannot receive new orders. Existing buyer obligations are either fulfilled or refunded. Settlement is held.

---

### UC-10 — System Reconciles Payments Daily

| | |
|---|---|
| **Actor** | System, Finance Admin |
| **Goal** | Prove that recorded transactions match the provider's record |
| **Preconditions** | A settlement report is available from the provider |
| **Traces to** | BR-PAY-09, BR-LDG-05, BR-LDG-07, RPT-01 |

**Main flow**

1. System retrieves the provider's settlement report for the period.
2. System matches each provider transaction to a platform payment record on the provider reference.
3. System compares totals and per-transaction amounts.
4. Where everything matches, system marks the transactions Reconciled and posts the closing position.
5. System publishes the daily reconciliation report.

**Exception flows**

- **E1 — Provider transaction with no platform record.** Raised as an exception. Funds are not attributed to any vendor until resolved.
- **E2 — Platform record with no provider transaction.** Raised as an exception. The order is not progressed.
- **E3 — Amount mismatch.** Raised as an exception with both values shown.
- **E4 — Any unresolved exception at settlement cut-off.** Blocks the settlement run per UC-05 E1.

**Postconditions** — Every transaction is either Reconciled or in the exception queue with an owner. The trial balance reconciles to zero.

---

### UC-11 — System Emits a Tax Invoice

| | |
|---|---|
| **Actor** | System |
| **Goal** | Meet the electronic invoicing obligation for every confirmed sale |
| **Preconditions** | Payment is Confirmed |
| **Traces to** | REG-04 to REG-08 |

**Main flow**

1. Payment confirmation raises an event.
2. System composes the tax invoice from the order, applying the VAT treatment determined by the classification in Section 14.1.
3. System transmits the invoice to the revenue authority's electronic invoicing service.
4. System records the acknowledgement reference against the order.
5. Invoice is made available to the buyer.

**Exception flows**

- **E1 — Transmission fails.** System retries with backoff. The order is not blocked from fulfilment by a transmission failure.
- **E2 — Retries exhausted.** Invoice moves to the dead-letter queue, an operational alert is raised, and it appears in the admin exception queue for manual resolution.
- **E3 — Refund or cancellation.** System issues a credit note referencing the original invoice.

**Postconditions** — Every confirmed order has either a transmitted invoice with an acknowledgement, or an entry in the exception queue with an owner. No confirmed order is silently uninvoiced.

---

### UC-12 — Buyer Reviews a Purchase

| | |
|---|---|
| **Actor** | Buyer, Vendor, Platform Admin |
| **Goal** | Record and publish an authentic assessment |
| **Preconditions** | Buyer has a Delivered sub-order containing the product |
| **Traces to** | BR-REV-01 to BR-REV-06 |

**Main flow**

1. System invites the buyer to review after delivery.
2. Buyer submits a rating and optional text.
3. System validates the buyer has a delivered order for that product.
4. System publishes the review and updates the product and vendor aggregate ratings.
5. Vendor may respond publicly once.

**Exception flows**

- **E1 — No qualifying order.** Submission is refused.
- **E2 — Policy violation.** Admin removes the review with a logged reason. Aggregates are recalculated.
- **E3 — Vendor reviewing own product.** Blocked by the system.

**Postconditions** — Review is published and aggregates reflect it, or the submission is refused with a reason.

---

### 9.13 Use Case to Requirement Traceability

| Use case | Primary requirement groups |
|---|---|
| UC-01 Vendor registration | BR-IAM, BR-VND |
| UC-02 Product listing | BR-CAT, BR-INV |
| UC-03 Multi-vendor order | BR-CHK, BR-ORD, BR-PAY, BR-INV |
| UC-04 Fulfilment | BR-ORD, BR-FUL |
| UC-05 Settlement | BR-STL, BR-LDG |
| UC-06 Return and refund | BR-RTN, BR-PAY, BR-ORD |
| UC-07 Dispute resolution | BR-RTN, BR-ADM |
| UC-08 Discovery | BR-SCH, BR-CAT |
| UC-09 Vendor suspension | BR-VND, BR-ORD, BR-STL |
| UC-10 Reconciliation | BR-PAY, BR-LDG |
| UC-11 Tax invoicing | REG |
| UC-12 Reviews | BR-REV |

Every functional requirement group in Section 10 is exercised by at least one use case. Requirements not reachable from any use case are candidates for removal at review.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 10. Functional Requirements

Requirements are numbered by module.

**Priority is defined against launch**, which is the exit of Phase 9 in Section 18. The definitions are stated because a prioritisation in which everything is a Must gives RSK-12 nothing to enforce against.

| Priority | Meaning |
|---|---|
| **M** | Launch cannot proceed without it. Either money or regulatory correctness depends on it, or the core buyer or vendor journey does not function |
| **S** | Required within six months of launch. Its absence degrades the product but does not prevent trading, and it can be scheduled against observed demand |
| **C** | Desirable. Built only if a phase completes with capacity remaining |
| **W** | Not this release |

**Phase is stated per module below.** MoSCoW answers whether something is built; the phase answers when. Both are needed: a Must in Phase 8 may not be pulled into Phase 3 without a change request under Section 21, and this is the primary control against the scope expansion identified in RSK-12.

### 10.1 Identity and Access (BR-IAM)

**Delivered in Phase 1.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-IAM-01 | The system shall allow registration using phone number or email address | M |
| BR-IAM-02 | The system shall verify phone numbers by one-time code before allowing checkout | M |
| BR-IAM-03 | The system shall support role-based access control covering all actors in Section 8 | M |
| BR-IAM-04 | The system shall enforce that vendor staff cannot view or modify payout banking details | M |
| BR-IAM-05 | The system shall support password reset via verified phone or email | M |
| BR-IAM-06 | The system shall log all authentication events with timestamp, actor, and source address | M |
| BR-IAM-07 | The system shall require multi-factor authentication for all admin and finance roles | M |
| BR-IAM-08 | The system shall support guest browsing without registration, requiring registration only at checkout | S |
| BR-IAM-09 | The system shall automatically lock accounts after a configurable number of failed attempts | S |

### 10.2 Vendor Onboarding and Management (BR-VND)

**Delivered in Phase 2.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-VND-01 | The system shall allow prospective vendors to self-register and submit an application | M |
| BR-VND-02 | The system shall capture business name, registration number, KRA PIN, physical address, and contact details | M |
| BR-VND-03 | The system shall allow upload of verification documents including national ID, business registration certificate, and KRA PIN certificate | M |
| BR-VND-04 | The system shall route completed applications to an admin approval queue | M |
| BR-VND-05 | The system shall support application states: Draft, Submitted, Under Review, Information Requested, Approved, Rejected, Suspended | M |
| BR-VND-06 | The system shall notify applicants of state changes with reason where rejected or suspended | M |
| BR-VND-07 | The system shall prevent an unapproved vendor from publishing products or receiving orders | M |
| BR-VND-08 | The system shall capture and verify vendor payout details (M-Pesa number or bank account) separately from the application | M |
| BR-VND-09 | The system shall require re-verification when payout details are changed, with a mandatory cooling-off period before the next settlement | M |
| BR-VND-10 | The system shall allow admins to suspend a vendor, immediately delisting their products while preserving in-flight orders | M |
| BR-VND-11 | The system shall provide a vendor storefront profile with business name, logo, description, and rating | S |
| BR-VND-12 | The system shall allow vendors to set operating hours and holiday mode, suppressing new orders when closed | S |
| BR-VND-13 | The system shall track vendor performance metrics: fulfilment rate, cancellation rate, average dispatch time, rating | S |

### 10.3 Catalog (BR-CAT)

**Delivered in Phases 1 and 2** — minimal listing in the walking skeleton, full lifecycle with vendor onboarding.

| Ref | Requirement | Priority |
|---|---|---|
| BR-CAT-01 | The system shall allow approved vendors to create, edit, and delist products | M |
| BR-CAT-02 | The system shall scope every product to exactly one owning vendor | M |
| BR-CAT-03 | The system shall support a hierarchical category taxonomy maintained by admins only | M |
| BR-CAT-04 | The system shall support product variants (for example size and colour) with independent price and stock | M |
| BR-CAT-05 | The system shall require a minimum of one image per published product | M |
| BR-CAT-06 | The system shall support product states: Draft, Pending Review, Active, Out of Stock, Delisted, Rejected | M |
| BR-CAT-07 | The system shall allow admins to review and reject products with a stated reason | M |
| BR-CAT-08 | The system shall maintain a prohibited-items list and block listings matching it | M |
| BR-CAT-09 | The system shall record the full history of price changes for each product | S |
| BR-CAT-10 | The system shall support bulk product upload by spreadsheet with validation and an error report | S |
| BR-CAT-11 | The system shall prevent a vendor from viewing or modifying another vendor's products under any circumstance | M |
| BR-CAT-12 | The system shall support product attributes that vary by category | S |

### 10.4 Inventory (BR-INV)

**Delivered in Phase 2.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-INV-01 | The system shall track available stock quantity per product variant | M |
| BR-INV-02 | The system shall reserve stock at the point of checkout initiation, for a configurable expiry period | M |
| BR-INV-03 | The system shall release reserved stock automatically when the reservation expires or payment fails | M |
| BR-INV-04 | The system shall prevent overselling by rejecting reservations that exceed available quantity | M |
| BR-INV-05 | The system shall convert reservation to deduction only upon payment confirmation | M |
| BR-INV-06 | The system shall return stock to available upon cancellation or accepted return | M |
| BR-INV-07 | The system shall notify vendors when stock for a product falls below a configurable threshold | S |
| BR-INV-08 | The system shall maintain an auditable log of every stock movement with reason and reference | M |

### 10.5 Cart and Checkout (BR-CHK)

**Delivered in Phase 4.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-CHK-01 | The system shall allow a buyer to hold items from multiple vendors in a single cart | M |
| BR-CHK-02 | The system shall display the cart grouped by vendor, showing per-vendor delivery cost | M |
| BR-CHK-03 | The system shall persist the cart across sessions for authenticated buyers | S |
| BR-CHK-04 | The system shall revalidate price and stock availability at checkout and notify the buyer of any change before payment | M |
| BR-CHK-05 | The system shall allow selection of delivery method per vendor group: door delivery or pickup point | M |
| BR-CHK-06 | The system shall calculate delivery cost per vendor group based on origin, destination, and package characteristics | M |
| BR-CHK-07 | The system shall display a full cost breakdown before payment: items, delivery, taxes, total | M |
| BR-CHK-08 | The system shall support saved delivery addresses and pickup point selection | M |
| BR-CHK-09 | The system shall prevent checkout where any item's vendor is suspended or closed | M |
| BR-CHK-10 | The system shall support promotional discount codes with scope, expiry, and usage limits | C |

### 10.6 Orders (BR-ORD)

**Delivered in Phases 1 and 4** — single-vendor order in the walking skeleton, splitting and partial cancellation with multi-vendor mechanics.

| Ref | Requirement | Priority |
|---|---|---|
| BR-ORD-01 | The system shall create a parent order on payment initiation, with a unique reference in the format MER-XXXXXXXX | M |
| BR-ORD-02 | The system shall split the parent order into one sub-order per vendor, each independently trackable | M |
| BR-ORD-03 | The system shall maintain independent state for each sub-order: Pending Payment, Confirmed, Accepted, Dispatched, In Transit, Delivered, Cancelled, Returned | M |
| BR-ORD-04 | The system shall derive parent order status from the aggregate of its sub-orders | M |
| BR-ORD-05 | The system shall require vendor acceptance of a sub-order within a configurable window, auto-cancelling if not accepted | M |
| BR-ORD-06 | The system shall support partial cancellation at sub-order level without affecting other sub-orders | M |
| BR-ORD-07 | The system shall support partial refund calculated correctly against the affected sub-order only | M |
| BR-ORD-08 | The system shall notify buyer and vendor at each state transition | M |
| BR-ORD-09 | The system shall provide buyers with an order history showing per-vendor status | M |
| BR-ORD-10 | The system shall maintain an immutable audit trail of every order state change with actor and timestamp | M |
| BR-ORD-11 | The system shall allow admins to intervene in any order state with mandatory reason capture | M |
| BR-ORD-12 | The system shall handle the case where one sub-order is cancelled after payment by triggering a partial refund automatically | M |
| BR-ORD-13 | The system shall allow a buyer to cancel a sub-order without vendor approval at any point before that sub-order enters Dispatched | M |
| BR-ORD-14 | The system shall, on buyer cancellation, initiate refund of the affected sub-order under BR-ORD-07, return stock under BR-INV-06, and reverse commission under BR-RTN-05, without affecting any other sub-order of the same parent order | M |
| BR-ORD-15 | The system shall show the buyer, for each sub-order, whether cancellation remains available, and shall direct the buyer to the return process in 10.11 once it does not | M |
| BR-ORD-16 | The system shall notify the vendor immediately on buyer cancellation, and shall not count a buyer-initiated cancellation against the vendor's cancellation rate under BR-VND-13 | S |

### 10.7 Payments (BR-PAY)

**Delivered in Phase 3.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-PAY-01 | The system shall collect payment through a CBK-licensed payment aggregator; it shall not hold customer funds directly | M |
| BR-PAY-02 | The system shall support M-Pesa STK Push as the primary payment method | M |
| BR-PAY-03 | The system shall support card payment as a secondary method | S |
| BR-PAY-04 | The system shall treat a payment as confirmed only upon receipt of a verified provider callback, never upon prompt initiation | M |
| BR-PAY-05 | The system shall process every payment callback idempotently, keyed on the provider transaction identifier | M |
| BR-PAY-06 | The system shall independently poll transaction status where a callback is not received within a defined window | M |
| BR-PAY-07 | The system shall maintain explicit payment states: Initiated, Awaiting Confirmation, Confirmed, Failed, Timed Out, Reversed, Reconciled | M |
| BR-PAY-08 | The system shall handle transactions exceeding the M-Pesa per-transaction limit of KES 150,000 by offering an alternative payment path rather than failing at checkout | S |
| BR-PAY-09 | The system shall reconcile all transactions against the provider's settlement report on a daily cycle and flag every discrepancy | M |
| BR-PAY-10 | The system shall never permit an order to progress to fulfilment on an unconfirmed payment | M |
| BR-PAY-11 | The system shall support refund initiation through the provider with full traceability to the original transaction | M |
| BR-PAY-12 | The system shall capture and store the provider's transaction reference against the order for buyer and support reference | M |

### 10.8 Ledger (BR-LDG)

**Delivered in Phase 3.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-LDG-01 | The system shall maintain a double-entry ledger recording every movement of value | M |
| BR-LDG-02 | The system shall record entries for: buyer collection, commission earned, delivery cost, vendor payable, holdback, refunds, and settlement disbursement | M |
| BR-LDG-03 | The system shall guarantee that ledger entries are written atomically with the transaction that causes them | M |
| BR-LDG-04 | The system shall not permit modification or deletion of a posted ledger entry; corrections shall be made by contra entry only | M |
| BR-LDG-05 | The system shall produce a trial balance that reconciles to zero at any point in time | M |
| BR-LDG-06 | The system shall maintain a per-vendor account balance derivable entirely from ledger entries | M |
| BR-LDG-07 | The system shall reconcile the ledger against the payment provider's balance daily and raise an alert on any variance | M |

### 10.9 Settlement and Payouts (BR-STL)

**Delivered in Phase 6**, after returns in Phase 5 — see Section 18.

| Ref | Requirement | Priority |
|---|---|---|
| BR-STL-01 | The system shall calculate vendor entitlement as gross sales less commission, less fees, less refunds, less holdback | M |
| BR-STL-02 | The system shall run settlement on a configurable cycle | M |
| BR-STL-03 | The system shall generate a vendor statement for each settlement period, itemised by order | M |
| BR-STL-04 | The system shall withhold settlement on orders still within the return window | M |
| BR-STL-05 | The system shall release holdback automatically once the return window closes without a claim | M |
| BR-STL-06 | The system shall instruct disbursement through the payment provider and record the outcome against the settlement | M |
| BR-STL-07 | The system shall handle failed disbursements by retrying and escalating to finance after a defined number of attempts | M |
| BR-STL-08 | The system shall prevent settlement to a vendor whose payout details changed within the cooling-off period | M |
| BR-STL-09 | The system shall support manual settlement hold on an individual vendor with reason capture | M |
| BR-STL-10 | The system shall produce a reconciliation report before any settlement run is executed | M |
| BR-STL-11 | The system shall allow finance to review and approve a settlement run before disbursement is instructed | M |
| BR-STL-12 | The system shall attach a platform-generated idempotency key to every disbursement instruction, and shall guarantee that a settlement line results in at most one disbursement regardless of retry, resumption, or duplicate provider response | M |

### 10.10 Fulfilment and Logistics (BR-FUL)

**Delivered in Phase 7.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-FUL-01 | The system shall integrate with at least one third-party logistics provider for rate quotation, dispatch, and tracking | M |
| BR-FUL-02 | The system shall support pickup-point delivery as a first-class option alongside door delivery | M |
| BR-FUL-03 | The system shall support multiple carriers behind a common interface, allowing carriers to be added without changes to order processing | M |
| BR-FUL-04 | The system shall generate or retrieve a tracking reference on dispatch and expose it to the buyer | M |
| BR-FUL-05 | The system shall update sub-order status automatically from carrier tracking events where available | S |
| BR-FUL-06 | The system shall support manual status update by the vendor where carrier integration is unavailable | M |
| BR-FUL-07 | The system shall record proof of delivery where the carrier provides it | S |
| BR-FUL-08 | The system shall allow admins to configure delivery zones and rate cards | M |

### 10.11 Returns, Refunds and Disputes (BR-RTN)

**Delivered in Phase 5**, before settlement — see Section 18.

| Ref | Requirement | Priority |
|---|---|---|
| BR-RTN-01 | The system shall allow buyers to raise a return request within a configurable window from delivery | M |
| BR-RTN-02 | The system shall capture return reason from a controlled list, with supporting images | M |
| BR-RTN-03 | The system shall route return requests to the vendor for acceptance, with escalation to admin on rejection or timeout | M |
| BR-RTN-04 | The system shall support full and partial refunds | M |
| BR-RTN-05 | The system shall reverse commission proportionally on any refund | M |
| BR-RTN-06 | The system shall provide admins with a dispute queue showing full order, payment, and communication history | M |
| BR-RTN-07 | The system shall allow admins to resolve a dispute in favour of either party with mandatory reason capture | M |
| BR-RTN-08 | The system shall recover refunded amounts from future vendor settlements where holdback is insufficient | M |
| BR-RTN-09 | The system shall track return rate by vendor and by product and surface outliers to admins | S |

### 10.12 Reviews and Ratings (BR-REV)

**Delivered in Phase 8.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-REV-01 | The system shall permit reviews only from buyers with a delivered order for that product | S |
| BR-REV-02 | The system shall support a numeric rating and free-text review | S |
| BR-REV-03 | The system shall aggregate ratings to product level and vendor level | S |
| BR-REV-04 | The system shall allow vendors to respond publicly to a review once | S |
| BR-REV-05 | The system shall allow admins to remove reviews violating content policy, with reason logged | S |
| BR-REV-06 | The system shall prevent a vendor from reviewing their own products | S |

Reviews are re-baselined to Should in this version. At launch scale — 250 vendors under active admin moderation, with vendor suspension available under BR-VND-10 — trust is enforced administratively rather than through buyer reputation, and the platform can trade without reviews. **If reviews ship, BR-REV-01, BR-REV-05 and BR-REV-06 are not optional within them:** an unverified review system is worse than none, because it invites exactly the manipulation the ratings are meant to displace. BR-SCH-02's rating filter and BR-VND-11's storefront rating depend on this module and carry the same priority.

### 10.13 Search and Discovery (BR-SCH)

**Delivered in Phase 8.**

| Ref | Requirement | Priority |
|---|---|---|
| BR-SCH-01 | The system shall provide keyword search across product title, description, and attributes | M |
| BR-SCH-02 | The system shall support filtering by category, price range, vendor, rating, and delivery availability | S |
| BR-SCH-03 | The system shall support sorting by relevance, price, rating, and recency | M |
| BR-SCH-04 | The system shall exclude out-of-stock, delisted, and suspended-vendor products from results by default | M |
| BR-SCH-05 | The system shall handle common misspellings and partial matches | S |
| BR-SCH-06 | The system shall log search terms returning no results for merchandising review | S |

### 10.14 Notifications (BR-NTF)

**Delivered from Phase 2 onward**, extended as each subsequent module introduces new events.

| Ref | Requirement | Priority |
|---|---|---|
| BR-NTF-01 | The system shall send SMS notifications for order confirmation, dispatch, and delivery | M |
| BR-NTF-02 | The system shall send vendor notifications for new orders and settlement completion | M |
| BR-NTF-03 | The system shall support email as a secondary channel | S |
| BR-NTF-04 | The system shall allow users to opt out of marketing communications while retaining transactional notifications | M |
| BR-NTF-05 | The system shall retry failed notification delivery and log permanent failures | M |

### 10.15 Administration and Back-Office (BR-ADM)

**Delivered from Phase 2 onward** — approval queue in Phase 2, payment exception queue in Phase 3, dispute queue in Phase 5, configuration management in Phase 6.

| Ref | Requirement | Priority |
|---|---|---|
| BR-ADM-01 | The system shall provide a vendor approval queue with document viewing | M |
| BR-ADM-02 | The system shall provide order search across all vendors by reference, buyer, phone, or payment reference | M |
| BR-ADM-03 | The system shall provide a payment exception queue listing unreconciled, timed-out, and failed transactions | M |
| BR-ADM-04 | The system shall provide a dispute management queue | M |
| BR-ADM-05 | The system shall provide catalog moderation tools | S |
| BR-ADM-06 | The system shall provide configuration management for commission rates, delivery zones, holdback percentage, and settlement cycle | M |
| BR-ADM-07 | The system shall record every administrative action in an immutable audit log | M |
| BR-ADM-08 | The system shall allow admin impersonation of a vendor account for support purposes, with the impersonation logged and visible to the vendor | C |

### 10.16 Platform Integrity and Abuse Controls (BR-SEC)

**Delivered in Phase 9**, with BR-SEC-01 and BR-SEC-02 brought forward to Phase 1 alongside the authentication they protect.

These requirements exist because RSK-07, RSK-08 and RSK-11 name detection and control measures as mitigations, and Phase 9 schedules the work, but v2.1 stated no requirement for any of it. Under Section 6, a mitigation with no requirement behind it is out of scope.

| Ref | Requirement | Priority |
|---|---|---|
| BR-SEC-01 | The system shall rate-limit authentication attempts, one-time-code requests, password resets, checkout initiation and search, by account and by source address, with configurable thresholds | M |
| BR-SEC-02 | The system shall throttle one-time-code issuance per phone number and enforce a minimum interval between requests, to bound both abuse and SMS cost | M |
| BR-SEC-03 | The system shall record device and session characteristics against each order sufficient to identify repeated activity from a single origin | S |
| BR-SEC-04 | The system shall detect and surface to admins buyer-side patterns indicating abuse, including return rate, dispute rate and cancellation rate materially above the platform median | S |
| BR-SEC-05 | The system shall detect and surface to admins vendor-side patterns indicating fraud, including dispatch failure rate, stock-out rate on accepted orders, and payout detail changes within a configurable period before a settlement run | S |
| BR-SEC-06 | The system shall detect and alert on overselling events, being any confirmed order line exceeding recorded available stock at the point of deduction | M |
| BR-SEC-07 | The system shall apply automated controls as reversible restrictions only. No automated process shall permanently terminate a buyer or vendor account, and every automated restriction shall be visible to an admin with the triggering signal recorded | M |
| BR-SEC-08 | The system shall log every automated restriction and every manual override with actor, timestamp, reason and affected account, in the immutable audit log under BR-ADM-07 | M |

BR-SEC-07 is a deliberate constraint on the rest of this module. Automated fraud controls acting on incomplete signals will suspend legitimate vendors, and a vendor wrongly cut off from settlement has no recourse inside the product unless reversal is designed in from the start. Detection is automated; consequence is not.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 11. Acceptance Criteria

### 11.1 Purpose and Use

A requirement in Section 10 states what the system must do. An acceptance criterion states how the parties will agree it does. Criteria are written in Given / When / Then form so that they are directly testable and so that neither party can reasonably dispute whether a requirement has been met.

**Rules of acceptance**

| Ref | Rule |
|---|---|
| ACC-R1 | A requirement is accepted only when every acceptance criterion referencing it passes |
| ACC-R2 | Criteria marked **Critical** are release-blocking; no phase exits with a failing Critical criterion |
| ACC-R3 | A criterion is verified in a staging environment configured identically to production |
| ACC-R4 | Financial criteria are verified against the ledger, never against a user interface display |
| ACC-R5 | Where a criterion cannot be verified without a third party, the third party's sandbox is acceptable for phase exit but production verification is required before launch |

---

### 11.2 Vendor Onboarding

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-VND-01 | Given a submitted application, when an admin approves it, then the vendor's state becomes Approved and the vendor receives a notification within 60 seconds | BR-VND-04, BR-VND-06 | Standard |
| AC-VND-02 | Given a vendor in any state other than Approved, when that vendor attempts to publish a product, then the system refuses and no product becomes visible to buyers | BR-VND-07 | Critical |
| AC-VND-03 | Given an approved vendor, when a Vendor Staff user attempts to view payout banking details, then the system refuses and the attempt is logged | BR-VND-08, BR-IAM-04 | Critical |
| AC-VND-04 | Given a vendor who changed payout details 3 days ago and a cooling-off period of 7 days, when a settlement run executes, then that vendor is excluded and the exclusion reason is recorded | BR-VND-09, BR-STL-08 | Critical |
| AC-VND-05 | Given an application with a KRA PIN already held by an approved vendor, when it is submitted, then submission is refused with an explanatory message | BR-VND-02 | Standard |
| AC-VND-06 | Given an admin rejects an application, when the rejection is saved without a reason, then the system refuses the action | BR-VND-06 | Standard |

---

### 11.3 Catalog and Inventory

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-CAT-01 | Given vendor A is authenticated, when vendor A requests any product owned by vendor B by direct identifier, then the system returns not-found and logs the attempt | BR-CAT-11 | Critical |
| AC-CAT-02 | Given a product submitted without an image, when the vendor attempts to publish, then the system refuses | BR-CAT-05 | Standard |
| AC-CAT-03 | Given a listing whose title matches the prohibited-items list, when it is submitted, then submission is blocked and the matched term is shown | BR-CAT-08 | Critical |
| AC-CAT-04 | Given a product price is changed, when the change is saved, then the previous price, new price, actor and timestamp are retrievable | BR-CAT-09 | Standard |
| AC-INV-01 | Given a variant with 1 unit available, when two buyers reach checkout simultaneously, then exactly one reservation succeeds and the other is refused | BR-INV-04 | Critical |
| AC-INV-02 | Given a reservation with a 15-minute expiry, when 15 minutes elapse without payment confirmation, then the stock returns to available and the reservation is closed | BR-INV-03 | Critical |
| AC-INV-03 | Given a confirmed payment, when stock is deducted, then the reservation is consumed rather than released, and total stock is reduced exactly once | BR-INV-05 | Critical |
| AC-INV-04 | Given any stock movement, when the ledger of movements is queried, then every change has a reason and a reference to the causing transaction | BR-INV-08 | Standard |

---

### 11.4 Checkout and Orders

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-CHK-01 | Given a cart containing items from three vendors, when the buyer views the cart, then items are grouped into three vendor groups each showing its own delivery cost | BR-CHK-02 | Standard |
| AC-CHK-02 | Given a product price changed after the item was added to cart, when the buyer proceeds to checkout, then the buyer is shown the change and must explicitly accept before payment | BR-CHK-04 | Critical |
| AC-CHK-03 | Given a cart containing an item from a suspended vendor, when the buyer proceeds to checkout, then checkout is blocked and the affected group is identified | BR-CHK-09 | Critical |
| AC-ORD-01 | Given a confirmed payment for a three-vendor cart, when the order is created, then exactly one parent order and exactly three sub-orders exist, each independently addressable | BR-ORD-02 | Critical |
| AC-ORD-02 | Given three sub-orders where one is Delivered and two are Dispatched, when the parent order status is queried, then it reflects the aggregate and not the status of any single sub-order | BR-ORD-04 | Standard |
| AC-ORD-03 | Given a sub-order not accepted within the acceptance window, when the window expires, then that sub-order auto-cancels, a partial refund is issued for it alone, and the other sub-orders are unaffected | BR-ORD-05, BR-ORD-06 | Critical |
| AC-ORD-04 | Given a parent order of KES 6,000 across two sub-orders of KES 4,000 and KES 2,000, when the KES 2,000 sub-order is cancelled, then the buyer is refunded exactly KES 2,000 plus that group's delivery, and commission is reversed on that portion only | BR-ORD-07, BR-ORD-12 | Critical |
| AC-ORD-05 | Given any order state change, when the audit trail is queried, then every transition shows actor, timestamp, previous state and new state, and no record can be modified | BR-ORD-10 | Critical |
| AC-ORD-06 | Given a paid parent order with sub-orders from vendors A and B, when the buyer cancels vendor A's sub-order before it is Dispatched, then vendor A's sub-order is refunded in full, its stock is returned, its commission is reversed, and vendor B's sub-order is unaffected in state, fulfilment and settlement | BR-ORD-13, BR-ORD-14 | Critical |
| AC-ORD-07 | Given a sub-order that has entered Dispatched, when the buyer views it, then cancellation is not offered and the buyer is directed to the return process | BR-ORD-13, BR-ORD-15 | Major |

---

### 11.5 Payments

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-PAY-01 | Given a payment prompt has been sent, when no callback has been received, then the order remains in Pending Payment and no vendor is notified and no stock is deducted | BR-PAY-04, BR-PAY-10 | Critical |
| AC-PAY-02 | Given a callback for transaction reference X has been processed, when an identical callback for reference X is received again, then no additional ledger entry is created, no duplicate order is created, and the system responds successfully | BR-PAY-05 | Critical |
| AC-PAY-03 | Given a payment initiated 10 minutes ago with no callback, when the polling window elapses, then the system queries the provider and resolves the payment to Confirmed or Failed without manual intervention | BR-PAY-06 | Critical |
| AC-PAY-04 | Given a cart total exceeding the M-Pesa per-transaction ceiling, when the buyer reaches payment, then an alternative payment path is offered and the buyer is not left unable to complete | BR-PAY-08 | Critical |
| AC-PAY-05 | Given the payment provider is unreachable, when a buyer attempts checkout, then the buyer receives a clear message, no order is created, and no stock remains reserved beyond expiry | BR-PAY-04, NFR-06 | Critical |
| AC-PAY-06 | Given a confirmed payment, when the order record is queried, then the provider transaction reference is present and is sufficient to locate the transaction in the provider's own records | BR-PAY-12 | Standard |
| AC-PAY-07 | Given a payment confirmed but sub-order generation failed, when the exception queue is reviewed, then the incident is present, the funds are accounted for, and no state exists in which the buyer has paid without either an order or a refund | BR-PAY-10 | Critical |

---

### 11.6 Ledger and Settlement

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-LDG-01 | Given any set of transactions in any state, when a trial balance is produced, then it sums to zero | BR-LDG-05 | Critical |
| AC-LDG-02 | Given a posted ledger entry, when any actor including a database administrator attempts to modify or delete it through the application, then the action is refused | BR-LDG-04 | Critical |
| AC-LDG-03 | Given a payment confirmation, when the transaction commits, then the payment record and its ledger entries are both present or both absent, never one without the other | BR-LDG-03 | Critical |
| AC-LDG-04 | Given a vendor's balance displayed anywhere in the system, when it is recomputed from ledger entries alone, then the two values are identical | BR-LDG-06 | Critical |
| AC-STL-01 | Given a vendor with KES 50,000 gross sales, 9% commission, KES 3,000 refunds and 10% holdback, when entitlement is calculated, then the amount is exactly KES 38,050 and each deduction is itemised on the statement | BR-STL-01, BR-STL-03 | Critical |
| AC-STL-02 | Given an unresolved reconciliation variance, when a settlement run is initiated, then the run is blocked in full and no vendor is disbursed | BR-STL-10, BR-LDG-07 | Critical |
| AC-STL-03 | Given an order still within its return window, when settlement runs, then that order's value is excluded from entitlement and remains held | BR-STL-04 | Critical |
| AC-STL-04 | Given a settlement run interrupted after disbursing 40 of 100 vendors, when the run resumes, then the 40 already disbursed are not paid again and the remaining 60 are processed | BR-STL-07, BR-STL-12 | Critical |
| AC-STL-05 | Given refunds exceeding a vendor's sales in a period, when settlement is calculated, then no disbursement is made and the negative balance carries forward as a recorded debit | BR-RTN-08 | Critical |
| AC-STL-06 | Given a settlement run has been calculated, when finance has not approved it, then no disbursement instruction is sent to the provider | BR-STL-11 | Critical |
| AC-STL-07 | Given a disbursement instruction that timed out without a provider response, when the instruction is retried under BR-STL-07 and the original transaction had in fact succeeded, then the provider recognises the idempotency key and the vendor is paid exactly once | BR-STL-07, BR-STL-12 | Critical |
| AC-STL-08 | Given an order placed before a commission rate change and settled after it, when the settlement line is inspected, then the rate applied is the one in force at order creation, the configuration version that produced it is identifiable, and re-running the calculation reproduces the same figure | COM-02, COM-05, DAT-07, DAT-08 | Critical |

---

### 11.7 Fulfilment

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-FUL-01 | Given a dispatched sub-order, when the buyer views the order, then a tracking reference is present and resolves to the carrier's tracking | BR-FUL-04 | Standard |
| AC-FUL-02 | Given a pickup-point delivery, when the parcel arrives at the agent point, then the buyer is notified and the state remains In Transit until collection is recorded | BR-FUL-02 | Standard |
| AC-FUL-03 | Given a second carrier is configured, when it is added, then no change is required to order processing logic and both carriers are selectable | BR-FUL-03 | Critical |
| AC-FUL-04 | Given a carrier booking request fails, when retries are exhausted, then the vendor is notified and a manual dispatch option is available | BR-FUL-06 | Standard |

---

### 11.8 Returns and Disputes

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-RTN-01 | Given a return raised on a two-item sub-order for one item only, when the refund is executed, then only that item's value and proportional commission are reversed | BR-RTN-04, BR-RTN-05 | Critical |
| AC-RTN-02 | Given a vendor rejects a return, when the rejection is recorded, then the case appears in the admin dispute queue with the full order, payment and message history | BR-RTN-03, BR-RTN-06 | Critical |
| AC-RTN-03 | Given a refund exceeding the vendor's available holdback, when it is executed, then the buyer is refunded in full and the shortfall is recorded as recoverable against future settlement | BR-RTN-08 | Critical |
| AC-RTN-04 | Given an admin resolves a dispute, when the resolution is saved without a reason, then the system refuses the action | BR-RTN-07 | Standard |
| AC-RTN-05 | Given a return raised outside the return window, when it is submitted, then it is refused with the window dates shown | BR-RTN-01 | Standard |

---

### 11.9 Regulatory

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-REG-01 | Given a confirmed order, when the invoice process completes, then a tax invoice exists with an acknowledgement reference from the revenue authority | REG-04 | Critical |
| AC-REG-02 | Given invoice transmission fails and retries are exhausted, when the exception queue is reviewed, then the failed invoice is present with the order reference and the failure reason | REG-05, REG-06 | Critical |
| AC-REG-03 | Given a refund is executed, when the tax records are reviewed, then a credit note exists referencing the original invoice | REG-08 | Critical |
| AC-REG-04 | Given a data subject requests erasure, when the request is actioned, then personal data is removed while transaction and ledger records required for statutory retention remain intact and anonymised | REG-13, DAT-05 | Critical |
| AC-REG-05 | Given a personal data breach is detected, when the incident process runs, then a reportable record is produced containing scope, data categories and affected subjects within 24 hours, allowing reporting inside 72 hours | REG-14 | Critical |
| AC-REG-06 | Given a buyer who opted out of marketing, when a marketing send executes, then that buyer is excluded while transactional notifications continue to be delivered | REG-15, BR-NTF-04 | Critical |
| AC-REG-07 | Given the platform holds no CBK licence, when any money movement is traced, then every collection and disbursement is executed by the licensed provider and no funds rest in a platform-controlled instrument outside the provider | REG-09, REG-10 | Critical |
| AC-REG-08 | Given a buyer with completed orders who requests erasure, when the request is executed, then the buyer's personal identifiers are no longer retrievable from any order, invoice or ledger record, while the trial balance under BR-LDG-05 is unchanged and every affected tax invoice remains intact and transmissible | REG-21, DAT-06 | Critical |
| AC-REG-09 | Given an erasure request naming a transaction inside its retention period, when deletion of that record is attempted, then the deletion is refused, the refusal states its lawful basis, and the request and its outcome are recorded | REG-23, REG-24 | Critical |

---

### 11.10 Non-Functional Acceptance

| Ref | Criterion | Traces to | Severity |
|---|---|---|---|
| AC-NFR-01 | Given a throttled 3G connection, when a category page with 40 products is loaded, then it renders within 2 seconds at the 95th percentile over 100 runs | NFR-01 | Critical |
| AC-NFR-02 | Given 10,000 concurrent simulated buyers, when browsing and checkout run for 30 minutes, then error rate stays below 0.5% and no ledger inconsistency arises | NFR-04 | Critical |
| AC-NFR-03 | Given a module boundary violation is introduced, when the build pipeline runs, then the build fails | NFR-14 | Critical |
| AC-NFR-04 | Given a full database loss, when the documented restore procedure is executed, then service is restored within 4 hours with no more than 15 minutes of data loss | NFR-13 | Critical |
| AC-NFR-05 | Given the buyer storefront, when audited against WCAG 2.1 Level AA, then no Level A or AA violations are present on the home, category, product, cart and checkout pages | NFR-15 | Standard |
| AC-NFR-06 | Given the payment provider is unavailable, when a buyer browses the catalog and builds a cart, then both function normally and only payment is degraded | NFR-07 | Critical |
| AC-NFR-07 | Given payout details and identity documents at rest, when storage is inspected directly, then the values are encrypted and not readable | NFR-08 | Critical |
| AC-SEC-01 | Given repeated one-time-code requests for a single phone number, when the configured threshold is exceeded, then further requests are refused for the configured interval, no SMS is sent, and the event is logged | BR-SEC-01, BR-SEC-02 | Critical |
| AC-SEC-02 | Given an account restricted by an automated control, when an admin reviews it, then the triggering signal is visible, the restriction can be lifted in full, and no automated process has terminated the account | BR-SEC-07, BR-SEC-08 | Critical |

---

### 11.11 Phase Exit Criteria

A phase is complete only when all of the following hold.

| Phase | Exit condition |
|---|---|
| 0 Foundations | Pipeline deploys to staging automatically; AC-NFR-03 passes; ADRs approved |
| 1 Walking skeleton | One order completes end to end in staging; observability visible for that order |
| 2 Vendor lifecycle | UC-01 and UC-02 pass in full including all exception flows; AC-VND-01 to AC-VND-06 and AC-CAT-01 to AC-CAT-04 pass |
| 3 Money | All Critical criteria in 11.5, 11.6 and AC-REG-01 to AC-REG-03 pass; trial balance reconciles across a 1,000-order synthetic load |
| 4 Multi-vendor mechanics | UC-03 passes in full; AC-ORD-01 to AC-ORD-05 pass |
| 5 Returns, refunds and disputes | UC-06 and UC-07 pass in full; AC-RTN-01 to AC-RTN-05 pass |
| 6 Settlement | UC-05 passes in full; a settlement run executes end to end with finance approval and reconciles |
| 7 Fulfilment | UC-04 passes against a live carrier in production configuration |
| 8 Discovery | UC-08 and UC-12 pass |
| 9 Hardening | All Critical criteria across Section 11 pass; AC-NFR-04 verified by an actual restore. **Launch gate.** |

---

### 11.12 Out-of-Scope Verification

The following are explicitly not acceptance conditions for this delivery and shall not be raised as defects.

| Item | Reason |
|---|---|
| Commercial performance against Section 5 projections | Depends on market factors outside the delivery |
| Vendor acquisition volume | A business activity, not a system function |
| Third-party provider outages | Handled by NFR-06 and NFR-07; provider uptime is not warranted |
| Content quality of vendor listings | Vendor responsibility, moderated not authored by the platform |
| Search result relevance judgements beyond BR-SCH-01 to BR-SCH-06 | Subjective; tuned iteratively post-launch |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 12. Non-Functional Requirements

| Ref | Category | Requirement |
|---|---|---|
| NFR-01 | Performance | Product listing and search pages shall render within 2 seconds at the 95th percentile on a 3G connection |
| NFR-02 | Performance | Checkout submission shall respond within 3 seconds excluding third-party payment latency |
| NFR-03 | Availability | The platform shall target 99.5% monthly availability excluding scheduled maintenance |
| NFR-04 | Scalability | The system shall support 10,000 concurrent buyers and 500,000 catalog items without architectural change |
| NFR-05 | Mobile | The buyer experience shall be mobile-first and function as an installable progressive web application |
| NFR-06 | Resilience | Payment provider unavailability shall not cause data loss; requests shall queue and reconcile |
| NFR-07 | Resilience | No single third-party outage shall prevent order browsing or cart building |
| NFR-08 | Security | All traffic shall be encrypted in transit; payout details and identity documents shall be encrypted at rest |
| NFR-09 | Security | The platform shall not store card data; card handling shall remain wholly with the payment provider |
| NFR-10 | Auditability | All financial and administrative actions shall be logged immutably and retained for seven years |
| NFR-11 | Observability | Structured logging, distributed tracing, and metrics shall be in place from first deployment |
| NFR-12 | Observability | Alerting shall cover payment reconciliation variance, settlement failure, and invoice transmission failure as priority incidents |
| NFR-13 | Recoverability | Recovery point objective of 15 minutes and recovery time objective of 4 hours; restore procedures shall be tested quarterly |
| NFR-14 | Maintainability | Module boundaries shall be enforced automatically in the build pipeline |
| NFR-15 | Accessibility | Buyer-facing pages shall meet WCAG 2.1 Level AA |
| NFR-16 | Localisation | The interface shall support English at launch, with the content model permitting Swahili addition without schema change |
| NFR-17 | Data residency | Personal data shall be stored in a manner consistent with Kenyan data protection requirements on cross-border transfer |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 13. Integration Requirements

| Ref | Integration | Purpose | Criticality |
|---|---|---|---|
| INT-01 | Payment aggregator (CBK-licensed) | Payment collection, refunds, vendor disbursement | Critical — no launch without it |
| INT-02 | KRA eTIMS | Electronic tax invoice generation and transmission | Critical — regulatory |
| INT-03 | Logistics provider(s) | Rate quotation, dispatch booking, tracking | Critical |
| INT-04 | SMS gateway | Transactional notifications | Critical |
| INT-05 | Email service | Secondary notifications, statements | High |
| INT-06 | Object storage | Product images, verification documents | Critical |
| INT-07 | Identity verification service | Automated ID and business registration validation | Medium — manual fallback acceptable at launch |

All integrations shall be implemented behind an internal interface such that the provider can be substituted without changes to business logic. Every outbound integration shall implement timeout, retry with backoff, and circuit breaking.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 14. Regulatory and Compliance Requirements

This section requires review and sign-off by a qualified Kenyan tax and legal advisor before development commences.

### 14.1 Value Added Tax — Principal vs Agent

| Ref | Requirement |
|---|---|
| REG-01 | The platform's VAT classification shall be formally determined by a tax advisor before pricing is finalised |
| REG-02 | The system shall be capable of calculating and reporting VAT on gross transaction value, not solely on commission |
| REG-03 | The commission and pricing model shall be validated against the principal classification before launch |

**Background.** In *Commissioner of Domestic Taxes v. Sendy Limited* (Income Tax Appeal E137 of 2024), decided 23 October 2025, the Kenya High Court held that digital platforms exercising significant control over transactions are deemed principal sellers for VAT purposes. The taxpayer had argued its liability was limited to commissions charged to drivers. The court found that because the platform set prices, dispatched service providers, issued payment requests as tax invoices, and collected payments, it was liable for VAT on the full customer payment. The judgment further held that private rulings issued by KRA cannot override statutory interpretation by a court.

The features that trigger this classification are the same features that create buyer trust. The business must decide knowingly whether to accept principal classification and price accordingly, or to restructure toward a genuine agency model at the cost of the escrow and control that make the platform valuable.

**This document has already made that decision.** Section 10 specifies, as Must-have requirements, every one of the four control factors the court relied on:

| Factor relied on in the judgment | Specified in this document by |
|---|---|
| The platform set prices | 5.2 — commission set by the platform with vendor and product-level overrides; 5.10 pricing governance |
| The platform dispatched service providers | BR-FUL-01, BR-FUL-03, BR-FUL-08 — the platform books carriers and configures rate cards |
| The platform issued payment requests as tax invoices | REG-04 — the platform generates the tax invoice for every confirmed order |
| The platform collected payments | BR-PAY-01, 5.9 — collection into a platform-controlled account, with holdback and settlement instructed by the platform |

**The consequence is that mitigation (2) in Section 5.6 — structural agency — is not available without re-baselining Section 10.** It is listed there as one of three live options, but the requirement set forecloses it: removing platform control of pricing, dispatch, invoicing and collection would remove escrow, multi-vendor checkout as a single payment, and the settlement engine, which is to say most of the product.

The requirements in this document therefore assume **principal classification**, and the model in 5.7 assumes mitigation (1) — a VAT-registered vendor base — as the means of containing the leakage. That assumption sits in direct tension with Section 4.5, which identifies social-native vendors, most of whom fall below the KES 5 million registration threshold, as the primary target segment. **Resolving that tension is a business decision, not a technical one**, and it is the substance of OPEN-01 and OPEN-02 taken together.

The purpose of stating this plainly is that a client signing Section 10 is, in effect, ratifying the principal position. That should be a knowing choice rather than an implication discovered later.

### 14.2 Electronic Tax Invoicing

| Ref | Requirement |
|---|---|
| REG-04 | The system shall generate a compliant electronic tax invoice for every confirmed order |
| REG-05 | Invoice transmission shall be implemented as a durable, retrying process with a dead-letter queue for failures |
| REG-06 | Invoice transmission failures shall raise an operational alert and shall be visible in the admin exception queue |
| REG-07 | The system shall retain transmitted invoices and their acknowledgement references |
| REG-08 | The system shall support credit notes for refunds and cancellations |

**Background.** From 1 January 2026, KRA's enhanced eTIMS requirements impose full digital traceability on e-commerce transactions, and un-invoiced online sales are non-deductible and may be flagged as undeclared income. KRA has introduced validation controls comparing declared returns against transaction data captured in eTIMS.

### 14.3 Payment Services Licensing

| Ref | Requirement |
|---|---|
| REG-09 | The platform shall not collect, hold, or disburse customer funds under its own account without a CBK licence |
| REG-10 | All collection and disbursement shall be executed through a CBK-licensed payment service provider |
| REG-11 | The commercial agreement with the payment provider shall be reviewed by legal counsel before integration begins |

**Background.** The National Payment Systems Act 2011 and the NPS Regulations 2014 require authorisation for entities providing payment services. CBK has taken enforcement action against unlicensed operators, including a cautionary notice to fintechs in late 2023 and earlier warnings to banks concerning specific providers. Licensed PSPs are additionally required to establish a trust arrangement to safeguard customer funds. Building on a licensed provider's rails avoids this entire obligation and is the basis of the architecture in this document.

### 14.4 Data Protection

| Ref | Requirement |
|---|---|
| REG-12 | The platform shall register with the Office of the Data Protection Commissioner as a data controller before processing begins |
| REG-13 | The system shall support data subject access, correction, and erasure requests |
| REG-21 | The system shall satisfy an erasure request affecting a record under statutory retention by irreversibly severing the personal identifiers from it, preserving the transaction, ledger, tax and audit record in full and in its original form |
| REG-22 | The system shall classify every category of personal data as erasable by deletion or erasable by severance, and shall apply the stated treatment consistently |
| REG-23 | The system shall refuse deletion of transaction, ledger, tax invoice and audit records within their retention period under 15.3, and shall inform the data subject of the lawful basis for the refusal |
| REG-24 | The system shall record, for every erasure request, the request reference, the categories acted upon, the treatment applied to each, the actor and the completion date |
| REG-14 | The system shall detect and support reporting of personal data breaches within 72 hours |
| REG-15 | The system shall capture and record consent for marketing communications separately from transactional consent |
| REG-16 | Verification documents shall be access-controlled, encrypted, and subject to a defined retention period |
| REG-17 | Vendor data shall be strictly segregated such that no vendor can access another vendor's or a buyer's data beyond their own orders |

**Background.** Under the Data Protection Act 2019 and the Registration Regulations 2021, registration is mandatory where annual turnover exceeds KES 5 million or the entity has more than 10 employees, and for entities in specified sectors regardless of size. Certificates are valid for 24 months. Penalties for non-compliance reach KES 5 million or 1% of annual turnover, whichever is lower. Breaches must be reported within 72 hours.

**Erasure against retained financial records.** REG-13 requires erasure. NFR-10 and DAT-05 require financial and audit records to be immutable, append-only and retained for seven years, and 15.3 requires transaction and tax records to be held for that period. These obligations conflict on any order belonging to a buyer who requests erasure, and v2.1 stated both without reconciling them, leaving the resolution to be improvised during implementation.

REG-21 to REG-24 state the resolution: the financial record survives, the person disappears from it. Erasure is effected by severing the link to the personal identifiers, not by editing or deleting the record — which is what makes the treatment compatible with DAT-05 rather than a breach of it. DAT-06 states the data design this depends on. The obligation to retain a record for tax purposes is a lawful basis for refusing its deletion; it is not a basis for retaining the buyer's name, phone number and address inside it indefinitely.

### 14.5 Consumer Protection

| Ref | Requirement |
|---|---|
| REG-18 | The platform shall publish clear terms of sale, return policy, and dispute procedure accessible before purchase |
| REG-19 | The platform shall display the total payable price inclusive of all charges before payment authorisation |
| REG-20 | The platform shall retain records sufficient to evidence the terms of any completed transaction |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 15. Data Requirements

### 15.1 Core Entities

Entities are grouped by owning module. Those marked **new in v2.2** were required by Section 10 but absent from the v2.1 list; they are recorded here because an entity omitted at this stage is a schema change later, and several of them carry constraints that cannot be retrofitted cheaply.

| Module | Entities |
|---|---|
| Identity | Buyer, Vendor User, Consent Record *(new)* |
| Vendor | Vendor, Vendor Application *(new)*, Verification Document *(new)*, Payout Detail *(new)* |
| Catalog | Product, Product Variant, Category, Price History *(new)* |
| Inventory | Inventory Record, Stock Reservation *(new)*, Stock Movement *(new)* |
| Checkout | Cart, Address *(new)*, Pickup Point *(new)* |
| Orders | Parent Order, Sub-Order, Order Line |
| Payments | Payment Transaction, Refund *(new)* |
| Ledger | Ledger Entry |
| Settlement | Settlement, Settlement Line |
| Fulfilment | Delivery, Carrier *(new)*, Delivery Zone *(new)*, Rate Card *(new)* |
| Returns | Return, Dispute, Credit Note *(new)* |
| Reviews | Review |
| Tax | Tax Invoice |
| Notifications | Notification *(new)* |
| Configuration | Commission Rate *(new)*, Configuration Version *(new)* |
| Cross-cutting | Audit Event, Erasure Request *(new)*, Account Restriction *(new)* |

Four of these carry design constraints worth stating, because each is the kind of omission that is inexpensive now and expensive after launch.

**Payout Detail is separate from Vendor.** BR-VND-08 captures and verifies it separately from the application, BR-VND-09 requires re-verification and a cooling-off period on change, and BR-STL-08 blocks settlement to a vendor whose details changed inside that period. None of that is expressible as a mutable field on Vendor: the entity needs its own verification state and an effective-from timestamp that settlement can interrogate.

**Commission Rate and Configuration Version are versioned entities, not settings.** COM-02 forbids retrospective application, COM-04 requires an audit of every change, and COM-05 requires reproducing, for any historical order, the exact rate applied and the configuration version that produced it. A configuration table that is updated in place cannot satisfy COM-05 at all. The same applies to the values under BR-ADM-06 — holdback percentage, settlement cycle and return window — each of which determines money owed and must therefore be reconstructible as at any past order. This is the single most consequential entry in this list.

**Refund is distinct from Return, and Credit Note from Tax Invoice.** A refund can arise without a return, through buyer cancellation under BR-ORD-13, vendor non-acceptance under BR-ORD-05, or admin dispute resolution under BR-RTN-07. Modelling refund as an attribute of Return leaves those three paths with nowhere to record the money movement that REG-08 then requires a credit note for.

**Stock Movement is distinct from Inventory Record.** BR-INV-08 requires an auditable log of every movement with reason and reference, which is an append-only series, while the Inventory Record is a current-state row. RSK-11 and BR-SEC-06 depend on the series, not the row.

### 15.2 Data Quality Requirements

| Ref | Requirement |
|---|---|
| DAT-01 | Every monetary amount shall be stored in minor units with an explicit currency; floating point representation is prohibited |
| DAT-02 | Every vendor-owned record shall carry an immutable vendor identifier enforced at the data access layer |
| DAT-03 | All timestamps shall be stored in UTC with the originating timezone recorded where relevant |
| DAT-04 | Phone numbers shall be normalised to international format on capture |
| DAT-05 | Financial and audit records shall be append-only |
| DAT-06 | Personal identifiers shall be held by reference from financial, tax and audit records rather than embedded within them, so that an erasure request under REG-21 can be satisfied by destroying the reference without modifying an append-only record |
| DAT-07 | Configuration that determines an amount owed — commission rates, holdback percentage, settlement cycle, return window and delivery rate cards — shall be versioned, with an effective-from timestamp, and shall never be updated in place |
| DAT-08 | Every order shall record the identifier of the configuration version in force at its creation, and that version shall be immutable once any order references it |

### 15.3 Retention

| Data type | Retention |
|---|---|
| Transaction and ledger records | 7 years |
| Tax invoices | 7 years |
| Vendor verification documents | Duration of relationship plus 7 years |
| Buyer personal data | Duration of account plus 2 years, subject to erasure requests |
| Application and access logs | 12 months |
| Marketing consent records | Duration of consent plus 2 years |

Retention periods above are the periods for which the *record* is held. Where a data subject exercises erasure under REG-13 during a retention period, the record is retained for the full period and the personal identifiers within it are severed under REG-21. Verification documents are the exception: they are held for the stated period as evidence of the onboarding decision and are erasable by deletion once it expires, per REG-22.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 16. Reporting Requirements

| Ref | Report | Audience | Frequency |
|---|---|---|---|
| RPT-01 | Daily payment reconciliation | Finance | Daily |
| RPT-02 | Settlement pre-run report | Finance | Per settlement cycle |
| RPT-03 | Vendor settlement statement | Vendor | Per settlement cycle |
| RPT-04 | VAT and invoice transmission summary | Finance | Monthly |
| RPT-05 | GMV, revenue, and take-rate summary | Management | Weekly and monthly |
| RPT-06 | Vendor performance league table | Operations | Weekly |
| RPT-07 | Return and dispute analysis | Operations | Weekly |
| RPT-08 | Vendor sales dashboard | Vendor | Real time |
| RPT-09 | Failed and unreconciled transaction exception report | Finance and Engineering | Daily |
| RPT-10 | Delivery performance by carrier | Operations | Weekly |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 17. Business Process Flows

### 17.1 Vendor Onboarding

Registration > document submission > application submitted > admin review > (information requested > resubmission) > approval > payout details capture and verification > product listing > first product review > active.

### 17.2 Order to Delivery

Cart build > checkout initiation > price and stock revalidation > stock reservation > parent order and per-vendor sub-orders created in Pending Payment > payment initiation > provider prompt > callback received and verified > payment confirmed > stock deducted > sub-orders move to Confirmed > tax invoice emitted > vendors notified > vendor acceptance > carrier dispatch booked > tracking issued > in transit > delivered > return window opens.

**Note on order creation.** The order record is created at payment *initiation*, not on confirmation, per BR-ORD-01. This is deliberate: the payment callback under BR-PAY-04 and BR-PAY-05 must attach to an existing order to be processed idempotently, and a callback arriving for an order that does not yet exist has nowhere to land. The Pending Payment state in BR-ORD-03 exists for exactly this window. Sub-orders are created with the parent rather than after confirmation, so that BR-ORD-03's per-sub-order state machine is well-defined from the outset.

### 17.3 Settlement

Return window closes > holdback released > settlement period cut-off > entitlement calculated per vendor > reconciliation report generated > finance review and approval > disbursement instructed to provider > outcome recorded > ledger updated > vendor statement issued > vendor notified.

### 17.4 Return and Refund

Buyer raises return within window > vendor notified > vendor accepts or rejects > (rejection escalates to admin dispute queue) > admin resolves > refund authorised > refund instructed through provider > commission reversed > stock restored where applicable > credit note issued > ledger updated > recovery applied against vendor holdback or future settlement.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 18. Delivery Roadmap

| Phase | Scope | Key modules | Exit criteria |
|---|---|---|---|
| 0 — Foundations | Architecture decisions, module skeleton, CI/CD, observability | — | Deployment pipeline operational; ADRs approved |
| 1 — Walking skeleton | Single vendor, single product, single order, manual payment marking, deployed end to end | IAM, Catalog, Orders | One order completes end to end in staging |
| 2 — Vendor lifecycle | Onboarding, approval, product management, inventory | Vendor, Catalog, Inventory | A vendor can self-onboard and list products |
| 3 — Money | Payment collection, ledger, tax invoicing, reconciliation | Payments, Ledger, Tax | Trial balance reconciles; invoices transmit successfully |
| 4 — Multi-vendor mechanics | Cart splitting, sub-orders, partial cancellation and refund | Cart, Checkout, Orders | Multi-vendor order splits and refunds correctly |
| 5 — Returns, refunds and disputes | Return window, return acceptance, commission reversal, dispute queue | Returns, Disputes | Return completes end to end; dispute resolvable in back-office |
| 6 — Settlement | Commission, holdback release, settlement runs, vendor statements | Settlement, Payouts | Full settlement cycle executes with finance approval |
| 7 — Fulfilment | Carrier integration, pickup points, tracking | Fulfilment | Orders dispatch and track through a live carrier |
| 8 — Discovery | Search, filtering, reviews and ratings | Search, Reviews | Buyer can find a product by keyword and filter; reviews publish |
| 9 — Hardening | Rate limiting, fraud controls, performance, restore testing | Cross-cutting | NFRs verified; restore tested |

**Launch is the exit of Phase 9, not Phase 6.** Hardening is a precondition of taking real money from real buyers, not a follow-up to it.

**Why returns precede settlement.** In v2.1 returns and disputes sat in Phase 7, after settlement in Phase 5. That sequence is not buildable: BR-STL-04 withholds settlement on orders still inside the return window, BR-STL-05 releases holdback when that window closes without a claim, and 17.3 begins the settlement flow with "return window closes > holdback released". Settlement cannot be exercised, let alone verified, before the return mechanism exists. Section 11.11 already recognised this implicitly by gating Phase 4 exit on AC-RTN-01 to AC-RTN-05. Returns are therefore delivered in Phase 5 and settlement in Phase 6.

**Schedule impact requires re-estimation.** The nine-month build estimate in 1.3 and the KES 10,800,000 engineering line in 5.8 were scoped to the previous definition of launch as Phases 0–6, which excluded returns, disputes, search, reviews and hardening — all of which are in scope under Section 6.1. Those figures are carried forward unchanged in this version and are understated. Re-estimation against the corrected sequence is recorded as OPEN-11.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 19. Risks

| Ref | Risk | Impact | Likelihood | Mitigation |
|---|---|---|---|---|
| RSK-01 | VAT assessed on gross value renders the commission model unprofitable | Critical | High | Obtain tax opinion before pricing; model both scenarios; build gross VAT capability regardless |
| RSK-02 | Insufficient vendor supply at launch | Critical | High | Secure signed commitments from a minimum vendor cohort before build completion |
| RSK-03 | Insufficient buyer demand; vendors churn | Critical | High | Vertical focus; concentrated launch marketing; measure repeat rate weekly |
| RSK-04 | Payment aggregator declines onboarding or withdraws split capability | High | Medium | Qualify two providers; abstract the integration behind an internal interface |
| RSK-05 | Ledger inconsistency causing incorrect vendor settlement | High | Medium | Double-entry from day one; daily reconciliation; block settlement on unreconciled variance |
| RSK-06 | eTIMS transmission failure creating tax exposure | High | Medium | Durable queue with retry; exception queue; daily transmission report |
| RSK-07 | Vendor fraud — listing goods not held, absconding after settlement | High | Medium | Holdback; settlement delay; verification; performance monitoring (BR-SEC-05); suspension capability (BR-VND-10) |
| RSK-08 | Buyer fraud — false return claims | Medium | Medium | Evidence requirement (BR-RTN-02); return rate monitoring (BR-RTN-09); account-level pattern detection (BR-SEC-04) |
| RSK-09 | Logistics partner failure degrades delivery reliability | High | Medium | Multi-carrier abstraction; per-carrier performance reporting |
| RSK-10 | Data breach involving personal or verification data | High | Low | Encryption at rest; access control; 72-hour breach process; regular review |
| RSK-11 | Overselling due to reservation logic failure | Medium | Medium | Reservation with expiry (BR-INV-02); database-level constraint (BR-INV-04); automated overselling alert (BR-SEC-06) |
| RSK-12 | Scope expansion delaying launch beyond runway | High | High | Strict MoSCoW enforcement; phase exit criteria; change control per Section 21 |
| RSK-13 | Regulatory change to platform taxation or payments licensing | High | Medium | Quarterly regulatory review with counsel; abstracted payment integration |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 20. Dependencies

| Ref | Dependency | Owner | Required by |
|---|---|---|---|
| DEP-01 | Company registration, KRA PIN, tax compliance certificate | Business Sponsor | Phase 0 |
| DEP-02 | Tax advisory opinion on principal vs agent classification | Legal / Tax Advisor | Phase 0 |
| DEP-03 | Payment aggregator commercial agreement and production credentials | Business Sponsor | Phase 3 |
| DEP-04 | eTIMS onboarding and credentials | Finance | Phase 3 |
| DEP-05 | ODPC registration | Business Sponsor | Before live processing |
| DEP-06 | Logistics partner agreement and API access | Operations | Phase 7 |
| DEP-07 | SMS gateway account with sender ID | Operations | Phase 2 |
| DEP-08 | Confirmed target category and commission schedule | Product Owner | Phase 2 |
| DEP-09 | Pilot vendor cohort recruited | Business Sponsor | Phase 6 |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 21. Change Control

Any change to requirements after sign-off shall be raised as a formal change request recording the requirement affected, business justification, and estimated impact on scope, schedule, and cost. Changes affecting Sections 10.7 through 10.9 or Section 14 require additional sign-off from Finance and the Legal / Tax Advisor. Changes shall be logged in the revision history and the document version incremented.

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 22. Open Issues

| Ref | Issue | Owner | Required by | Status |
|---|---|---|---|---|
| OPEN-01 | Target product category not yet selected; commission model and catalog schema depend on it | Product Owner | Before Phase 2 | Open |
| OPEN-02 | VAT classification unresolved; unit economics cannot be finalised | Legal / Tax Advisor | Before Phase 0 exit | Open |
| OPEN-03 | Payment aggregator not selected; two candidates to be qualified | Business Sponsor | Before Phase 3 | Open |
| OPEN-04 | Settlement cycle frequency not agreed | Finance | Before Phase 6 | Open |
| OPEN-05 | Holdback percentage and return window duration not agreed | Finance / Operations | Before Phase 5 | Open |
| OPEN-06 | Whether the platform sets delivery pricing, or vendors do — a control factor under 14.1 and therefore an input to OPEN-02, not a downstream consequence of it. Note that BR-CHK-06 and BR-FUL-08 as drafted already answer it in favour of platform control | Product Owner / Tax Advisor | Before Phase 0 exit | Open |
| OPEN-07 | Prohibited items list not drafted | Legal | Before Phase 2 | Open |
| OPEN-08 | Logistics partner not selected | Operations | Before Phase 7 | Open |
| OPEN-09 | Product name not selected. "Meridian" is a placeholder codename carrying no brand intent. Name must clear KIPI trademark search and .co.ke availability before any buyer-facing build begins | Business Sponsor | Before Phase 1 | Open |
| OPEN-10 | Whether funds may be collected into a platform-controlled account pending settlement, as described in 5.9, without triggering the licensing obligation that REG-09 avoids. The aggregator's licence covers the aggregator's activity; it does not automatically extend to an account under the platform's control | Legal / Tax Advisor | Before Phase 0 exit | Open |
| OPEN-11 | Build schedule and cost require re-estimation against the corrected phase sequence in Section 18. The nine-month, KES 10,800,000 estimate was scoped to launch at Phase 6 and excludes returns, disputes, search, reviews and hardening, all of which are in scope under 6.1 | Product Owner / Engineering | Before Phase 0 exit | Open |

---


```{=openxml}
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
```

## 23. Glossary

| Term | Definition |
|---|---|
| CBK | Central Bank of Kenya |
| eTIMS | Electronic Tax Invoice Management System operated by KRA |
| GMV | Gross Merchandise Value — total value of goods sold through the platform |
| Holdback | Portion of vendor entitlement retained against return and dispute risk |
| KRA | Kenya Revenue Authority |
| MoSCoW | Prioritisation method: Must, Should, Could, Won't |
| ODPC | Office of the Data Protection Commissioner |
| Parent order | The buyer-facing order encompassing all vendors in a single checkout |
| PSP | Payment Service Provider, as licensed under the National Payment Systems Act |
| Settlement | The process of calculating and disbursing amounts owed to vendors |
| STK Push | M-Pesa payment prompt initiated to a customer's handset |
| Sub-order | The per-vendor portion of a parent order, independently fulfilled and tracked |
| Take rate | Platform revenue as a percentage of GMV |

---

*End of document.*
