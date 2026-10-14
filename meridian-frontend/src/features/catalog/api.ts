import "server-only";

import {
  api,
  apiFetchEnvelope,
  type PageRequest,
  type PaginatedResponse,
} from "@/shared/api";
import { CACHE_TAGS, PUBLIC_REVALIDATE_SECONDS } from "@/shared/cache/tags";
import type { PriceChange, Product, ProductImage } from "./types";

/**
 * BR-IAM-08: browsing needs no session, so this is one of the few reads that can be cached
 * and shared between visitors. `auth: false` also keeps the call out of request scope, which
 * is what lets a page built on it stay prerenderable.
 */
export async function listProducts({
  page = 0,
  perPage = 20,
}: PageRequest = {}): Promise<PaginatedResponse<Product>> {
  return apiFetchEnvelope<PaginatedResponse<Product>>("/catalog/products", {
    query: { page, perPage },
    auth: false,
    tags: [CACHE_TAGS.catalogProducts],
    revalidate: PUBLIC_REVALIDATE_SECONDS,
  });
}

export async function getProduct(productId: string): Promise<Product> {
  return api.get<Product>(`/catalog/products/${productId}`, {
    auth: false,
    tags: [CACHE_TAGS.catalogProduct(productId), CACHE_TAGS.catalogProducts],
    revalidate: PUBLIC_REVALIDATE_SECONDS,
  });
}

/**
 * The signed-in vendor's own listings, drafts included.
 *
 * Sent authenticated even though `/catalog/products/**` is permitted unauthenticated — the
 * handler reads the caller's account to decide whose listings these are, so a tokenless call
 * has no answer to give. Never cached: the response is one vendor's private view.
 */
export async function listMyProducts(): Promise<Product[]> {
  return api.get<Product[]>("/catalog/products/mine");
}

export async function getMyProduct(productId: string): Promise<Product> {
  return api.get<Product>(`/catalog/products/mine/${productId}`);
}

/**
 * A product's images, in upload order. `url` is rewritten to this app's own `/img/**` proxy
 * (see that route's doc) rather than the backend-relative path the API returns, so the browser
 * never needs to reach the backend origin directly.
 */
export async function listProductImages(
  productId: string,
): Promise<ProductImage[]> {
  const images = await api.get<
    Array<{
      id: string;
      contentType: string;
      sortOrder: number;
      url: string;
    }>
  >(`/catalog/products/${productId}/images`, {
    auth: false,
    tags: [CACHE_TAGS.catalogProduct(productId)],
    revalidate: PUBLIC_REVALIDATE_SECONDS,
  });

  return images.map((image) => ({
    ...image,
    url: `/img${image.url}`,
  }));
}

/** AC-CAT-06: every price this vendor has charged for the listing. */
export async function getPriceHistory(
  productId: string,
): Promise<PriceChange[]> {
  return api.get<PriceChange[]>(
    `/catalog/products/mine/${productId}/price-history`,
  );
}
