"use server";

import { refresh, updateTag } from "next/cache";

import type { Product } from "@/features/catalog";
import type { DeliveryRateCard, DeliveryZone } from "@/features/fulfilment";
import type { Dispute } from "@/features/returns";
import type { Review } from "@/features/reviews";
import type { Vendor } from "@/features/vendor-account";
import type { VendorApplication } from "@/features/vendor-onboarding";
import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { CACHE_TAGS } from "@/shared/cache/tags";
import { requireSession } from "@/shared/lib/guard";
import { createSession } from "@/shared/lib/session";
import {
  applicationIdSchema,
  commissionRateSchema,
  createZoneSchema,
  holdbackRateSchema,
  impersonateVendorSchema,
  liftRestrictionSchema,
  moderateDelistSchema,
  rateCardSchema,
  rejectApplicationSchema,
  removeReviewSchema,
  requestInformationSchema,
  resolveDisputeSchema,
  suspendVendorSchema,
  zoneIdSchema,
} from "./schemas";
import type {
  CommissionRate,
  HoldbackRate,
  ImpersonationHandover,
  Restriction,
} from "./types";

const ADMIN_PATH = "/admin";

/**
 * Every action here is additionally role-gated by `@PreAuthorize` on the backend. The session
 * check below only establishes that someone is signed in — the authorisation decision is the
 * server's, taken per request against the live account, so a role cached on this side could
 * outlive a revoked one.
 */

// ---------------------------------------------------------------- pricing configuration

/** BR-PRC-01: a new commission version. Existing orders keep the version they were priced at. */
export async function setCommissionRate(
  input: unknown,
): Promise<ActionResult<CommissionRate>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(commissionRateSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not save that commission rate.", () =>
    api.post<CommissionRate>("/admin/config/commission-rate", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-PRC-02: a new holdback version, on the same versioned-configuration terms. */
export async function setHoldbackRate(
  input: unknown,
): Promise<ActionResult<HoldbackRate>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(holdbackRateSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not save that holdback rate.", () =>
    api.post<HoldbackRate>("/admin/config/holdback-rate", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}

// ---------------------------------------------------------------- catalog moderation

/** Takes a listing down over the vendor's head. The reason is recorded against the product. */
export async function moderateDelistProduct(
  input: unknown,
): Promise<ActionResult<Product>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(moderateDelistSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { productId, reason } = parsed.data;
  const result = await attempt("We could not delist that product.", () =>
    api.post<Product>(`/admin/catalog/products/${productId}/moderate-delist`, {
      reason,
    }),
  );

  if (result.status === "success") {
    updateTag(CACHE_TAGS.catalogProduct(productId));
    updateTag(CACHE_TAGS.catalogProducts);
    refresh();
  }
  return result;
}

/** BR-RVW-05: removes a review from the public listing without deleting the record. */
export async function removeReview(
  input: unknown,
): Promise<ActionResult<Review>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(removeReviewSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { reviewId, reason } = parsed.data;
  const result = await attempt("We could not remove that review.", () =>
    api.post<Review>(`/admin/reviews/${reviewId}/remove`, { reason }),
  );

  if (result.status === "success") {
    updateTag(CACHE_TAGS.productReviews(result.data.productId));
    updateTag(CACHE_TAGS.vendorReviews(result.data.vendorId));
    refresh();
  }
  return result;
}

// ---------------------------------------------------------------- fulfilment configuration

export async function createDeliveryZone(
  input: unknown,
): Promise<ActionResult<DeliveryZone>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(createZoneSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not create that zone.", () =>
    api.post<DeliveryZone>("/admin/fulfilment/zones", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}

/** Deactivation, not deletion — orders already routed through the zone still reference it. */
export async function deactivateDeliveryZone(
  input: unknown,
): Promise<ActionResult<void>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(zoneIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not deactivate that zone.", () =>
    api.post<void>(`/admin/fulfilment/zones/${parsed.data.zoneId}/deactivate`),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-FUL-01: what a carrier charges for a zone and method, from now on. */
export async function setDeliveryRateCard(
  input: unknown,
): Promise<ActionResult<DeliveryRateCard>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(rateCardSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not save that rate card.", () =>
    api.post<DeliveryRateCard>("/admin/fulfilment/rate-cards", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}

// ---------------------------------------------------------------- integrity

/** Lifts an automated restriction. The reason is what the audit log records as the override. */
export async function liftRestriction(
  input: unknown,
): Promise<ActionResult<Restriction>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(liftRestrictionSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { restrictionId, reason } = parsed.data;
  const result = await attempt("We could not lift that restriction.", () =>
    api.post<Restriction>(
      `/admin/integrity/restrictions/${restrictionId}/lift`,
      { reason },
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

// ---------------------------------------------------------------- returns

/** BR-RTN-06: finance decides a refused return, and the refund follows from the decision. */
export async function resolveDispute(
  input: unknown,
): Promise<ActionResult<Dispute>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(resolveDisputeSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { disputeId, ...resolution } = parsed.data;
  const result = await attempt("We could not resolve that dispute.", () =>
    api.post<Dispute>(
      `/admin/returns/disputes/${disputeId}/resolve`,
      resolution,
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

// ---------------------------------------------------------------- vendor applications

/** Moves a submitted application into review, which is what claims it off the queue. */
export async function moveApplicationToReview(
  input: unknown,
): Promise<ActionResult<VendorApplication>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(applicationIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not open that application.", () =>
    api.post<VendorApplication>(
      `/admin/vendor-applications/${parsed.data.applicationId}/under-review`,
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-VND-04: asks the applicant for something, pausing the review until they supply it. */
export async function requestApplicationInformation(
  input: unknown,
): Promise<ActionResult<VendorApplication>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(requestInformationSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { applicationId, note } = parsed.data;
  const result = await attempt("We could not send that request.", () =>
    api.post<VendorApplication>(
      `/admin/vendor-applications/${applicationId}/request-information`,
      { note },
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-VND-05: approval is what creates the vendor the applicant will trade as. */
export async function approveApplication(
  input: unknown,
): Promise<ActionResult<VendorApplication>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(applicationIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not approve that application.", () =>
    api.post<VendorApplication>(
      `/admin/vendor-applications/${parsed.data.applicationId}/approve`,
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

/** The rejection reason is sent to the applicant, so it is written for them, not for us. */
export async function rejectApplication(
  input: unknown,
): Promise<ActionResult<VendorApplication>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(rejectApplicationSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { applicationId, reason } = parsed.data;
  const result = await attempt("We could not reject that application.", () =>
    api.post<VendorApplication>(
      `/admin/vendor-applications/${applicationId}/reject`,
      { reason },
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-VND-10: suspends a trading vendor. Their listings stop being orderable immediately. */
export async function suspendVendor(
  input: unknown,
): Promise<ActionResult<Vendor>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(suspendVendorSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { vendorId, reason } = parsed.data;
  const result = await attempt("We could not suspend that vendor.", () =>
    api.post<Vendor>(`/admin/vendor-applications/vendors/${vendorId}/suspend`, {
      reason,
    }),
  );

  if (result.status === "success") {
    updateTag(CACHE_TAGS.catalogProducts);
    refresh();
  }
  return result;
}

// ---------------------------------------------------------------- impersonation

/**
 * BR-SEC: signs the admin in as a vendor, for support.
 *
 * The backend returns a session token. It is written straight into the httpOnly cookie and
 * never returned to the caller — a token that reaches a client component can be read by any
 * script on the page and is one `console.log` away from a log aggregator.
 *
 * This replaces the admin's own session cookie, so they act as the vendor until the
 * impersonation token expires and will need to sign in again afterwards. The vendor sees the
 * whole thing in their impersonation log either way, which is the point of the reason field.
 */
export async function impersonateVendor(
  input: unknown,
): Promise<ActionResult<ImpersonationHandover>> {
  await requireSession(ADMIN_PATH);

  const parsed = parseInput(impersonateVendorSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { vendorId, reason } = parsed.data;
  const result = await attempt("We could not start that session.", () =>
    api.post<{
      token: string;
      expiresAt: string;
      targetAccountId: string;
      impersonatorAccountId: string;
    }>(`/admin/vendors/${vendorId}/impersonate`, { reason }),
  );

  if (result.status === "error") return result;

  const { token, expiresAt, targetAccountId, impersonatorAccountId } =
    result.data;
  await createSession(token, expiresAt);
  refresh();

  return {
    status: "success",
    data: { targetAccountId, impersonatorAccountId, expiresAt },
  };
}
