/**
 * Cache tags shared between readers (Server Components) and writers (Server Actions).
 *
 * These cannot live in a `"use server"` module — such a file may only export async functions,
 * and exporting a plain constant from one strips every export off the module.
 *
 * Only the endpoints `SecurityConfiguration` marks `permitAll` are tagged at all. Everything
 * else travels with a bearer token, and caching a response that was authorised for one
 * account would hand it to the next caller.
 */

export const CACHE_TAGS = {
  /** Any change to the browsable catalog — a publish, a delisting, a price change. */
  catalogProducts: "catalog:products",
  catalogProduct: (productId: string) => `catalog:product:${productId}`,
  productReviews: (productId: string) => `reviews:product:${productId}`,
  vendorReviews: (vendorId: string) => `reviews:vendor:${vendorId}`,
} as const;

/** How long a public listing may be served before it is refetched, absent an explicit update. */
export const PUBLIC_REVALIDATE_SECONDS = 60;

/**
 * The caller's own vendor application. Nothing tagged with it is cached today — the
 * application is per-account and therefore uncacheable — but a Server Action that changes it
 * still calls `refresh()`, and this name keeps the two sides of that contract findable.
 */
export const VENDOR_APPLICATION_TAG = "vendor-application";
