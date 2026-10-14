import "server-only";

import type { Product } from "@/features/catalog";
import { apiFetchEnvelope, type PaginatedResponse } from "@/shared/api";
import { CACHE_TAGS, PUBLIC_REVALIDATE_SECONDS } from "@/shared/cache/tags";
import type { ProductSearchQuery } from "./types";

/**
 * BR-SCH-01/03: keyword and facet search over the public catalog.
 *
 * Unauthenticated like the catalog listing it searches, and tagged with the same
 * `catalogProducts` tag so publishing or delisting a product clears stale result pages
 * alongside the listing itself.
 *
 * The backend rate-limits this per source address (`SCOPE_SEARCH_SOURCE`) and answers a
 * breach with RATE_LIMITED — from a Server Component every visitor's search arrives from the
 * same host, so call it once per render rather than per keystroke.
 */
export async function searchProducts({
  keyword,
  minPriceMinorUnits,
  maxPriceMinorUnits,
  currency,
  vendorId,
  sort = "RELEVANCE",
  page = 0,
  perPage = 20,
}: ProductSearchQuery = {}): Promise<PaginatedResponse<Product>> {
  return apiFetchEnvelope<PaginatedResponse<Product>>("/search/products", {
    query: {
      keyword,
      minPriceMinorUnits,
      maxPriceMinorUnits,
      currency,
      vendorId,
      sort,
      page,
      perPage,
    },
    auth: false,
    tags: [CACHE_TAGS.catalogProducts],
    revalidate: PUBLIC_REVALIDATE_SECONDS,
  });
}
