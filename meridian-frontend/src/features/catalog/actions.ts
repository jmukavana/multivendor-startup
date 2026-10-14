"use server";

import { refresh, updateTag } from "next/cache";

import {
  type ActionResult,
  api,
  attempt,
  failed,
  parseInput,
} from "@/shared/api";
import { CACHE_TAGS } from "@/shared/cache/tags";
import { requireSession } from "@/shared/lib/guard";
import {
  changePriceSchema,
  createProductSchema,
  productIdSchema,
} from "./schemas";
import type { Product } from "./types";

const VENDOR_PRODUCTS_PATH = "/vendor/products";

/**
 * Publishes a catalog change to everyone.
 *
 * `updateTag` clears the shared, cached public listing; `refresh()` re-renders the vendor's
 * own page, whose reads are per-account and therefore never cached in the first place. Both
 * are needed — neither one covers the other's half.
 */
function publishCatalogChange(productId: string): void {
  updateTag(CACHE_TAGS.catalogProduct(productId));
  updateTag(CACHE_TAGS.catalogProducts);
  refresh();
}

/** BR-CAT-01: a new listing starts as a DRAFT, visible to nobody but its vendor. */
export async function createProduct(
  input: unknown,
): Promise<ActionResult<Product>> {
  await requireSession(VENDOR_PRODUCTS_PATH);

  const parsed = parseInput(createProductSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not create that listing.", () =>
    api.post<Product>("/catalog/products", parsed.data),
  );

  if (result.status === "success") publishCatalogChange(result.data.id);
  return result;
}

/** BR-CAT-02: DRAFT to ACTIVE, which is what makes the listing orderable. */
export async function publishProduct(
  input: unknown,
): Promise<ActionResult<Product>> {
  await requireSession(VENDOR_PRODUCTS_PATH);

  const parsed = parseInput(productIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not publish that listing.", () =>
    api.post<Product>(
      `/catalog/products/mine/${parsed.data.productId}/publish`,
    ),
  );

  if (result.status === "success") publishCatalogChange(result.data.id);
  return result;
}

/** BR-CAT-03: withdraws the listing from sale without deleting its order history. */
export async function delistProduct(
  input: unknown,
): Promise<ActionResult<Product>> {
  await requireSession(VENDOR_PRODUCTS_PATH);

  const parsed = parseInput(productIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not delist that listing.", () =>
    api.post<Product>(`/catalog/products/mine/${parsed.data.productId}/delist`),
  );

  if (result.status === "success") publishCatalogChange(result.data.id);
  return result;
}

/** BR-CAT-05/06: repricing is recorded, so it goes through the API rather than an edit. */
export async function changeProductPrice(
  input: unknown,
): Promise<ActionResult<Product>> {
  await requireSession(VENDOR_PRODUCTS_PATH);

  const parsed = parseInput(changePriceSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { productId, ...price } = parsed.data;
  const result = await attempt("We could not update that price.", () =>
    api.put<Product>(`/catalog/products/mine/${productId}/price`, price),
  );

  if (result.status === "success") publishCatalogChange(productId);
  return result;
}

/** The backend caps uploads at 5MB and rejects an empty file. */
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/**
 * Uploads a product image.
 *
 * Takes the raw FormData because a File cannot survive being copied into a plain object, and
 * forwards only the `file` part the backend's `@RequestParam` expects — never the caller's
 * whole form, which would leak any other field it happened to carry.
 */
export async function addProductImage(
  productId: string,
  formData: FormData,
): Promise<ActionResult<void>> {
  await requireSession(VENDOR_PRODUCTS_PATH);

  const parsed = parseInput(productIdSchema, { productId });
  if (!parsed.ok) return parsed.failure;

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return failed("Choose an image to upload.", "VALIDATION_ERROR", {
      file: ["Choose an image to upload"],
    });
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return failed("That image is larger than 5MB.", "VALIDATION_ERROR", {
      file: ["The image must be 5MB or smaller"],
    });
  }

  const upload = new FormData();
  upload.set("file", file);

  const result = await attempt("We could not upload that image.", () =>
    api.post<void>(
      `/catalog/products/mine/${parsed.data.productId}/images`,
      upload,
    ),
  );

  if (result.status === "success") publishCatalogChange(parsed.data.productId);
  return result;
}
