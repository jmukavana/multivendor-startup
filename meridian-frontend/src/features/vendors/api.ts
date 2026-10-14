import "server-only";

import { api } from "@/shared/api";
import type { VendorSummary } from "./types";

/**
 * Resolves vendor ids to display names, batched into one call — a product feed showing twenty
 * cards needs twenty names, and a request per card would be the N+1 this exists to avoid.
 *
 * Public, like the catalog listing it supports (BR-IAM-08). An id with no match — unknown, or
 * silently dropped by the backend — is simply missing from the result rather than an error, so
 * callers key their own lookup by id and fall back for whatever is absent.
 */
export async function listVendors(
  ids: readonly string[],
): Promise<VendorSummary[]> {
  const unique = [...new Set(ids)];
  if (unique.length === 0) return [];

  return api.get<VendorSummary[]>("/vendors", {
    auth: false,
    query: { ids: unique.join(",") },
  });
}

/** {@link listVendors}, indexed by id for O(1) lookup while rendering a list of cards. */
export async function resolveVendorNames(
  ids: readonly string[],
): Promise<Map<string, string>> {
  const vendors = await listVendors(ids);
  return new Map(vendors.map((vendor) => [vendor.id, vendor.businessName]));
}
