import "server-only";

import { api } from "@/shared/api";
import { CACHE_TAGS, PUBLIC_REVALIDATE_SECONDS } from "@/shared/cache/tags";
import type { RatingSummary, Review } from "./types";

/** BR-RVW: reviews sit under the product they belong to, and are public like it is. */
export async function listProductReviews(productId: string): Promise<Review[]> {
  return api.get<Review[]>(`/catalog/products/${productId}/reviews`, {
    auth: false,
    tags: [CACHE_TAGS.productReviews(productId)],
    revalidate: PUBLIC_REVALIDATE_SECONDS,
  });
}

export async function getProductRatingSummary(
  productId: string,
): Promise<RatingSummary> {
  return api.get<RatingSummary>(
    `/catalog/products/${productId}/reviews/summary`,
    {
      auth: false,
      tags: [CACHE_TAGS.productReviews(productId)],
      revalidate: PUBLIC_REVALIDATE_SECONDS,
    },
  );
}

/** BR-VND-11: a vendor's storefront rating, browsable without a session like their listings. */
export async function getVendorRatingSummary(
  vendorId: string,
): Promise<RatingSummary> {
  return api.get<RatingSummary>(`/vendors/${vendorId}/reviews/summary`, {
    auth: false,
    tags: [CACHE_TAGS.vendorReviews(vendorId)],
    revalidate: PUBLIC_REVALIDATE_SECONDS,
  });
}
