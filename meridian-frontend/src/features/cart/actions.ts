"use server";

import {
  getProduct,
  listProductImages,
  type Product,
} from "@/features/catalog";
import { resolveVendorNames } from "@/features/vendors";
import { API_ERROR_CODES, isApiError } from "@/shared/api";
import type { CartLine, CartLineDetail } from "./types";

async function productOrNull(productId: string): Promise<Product | null> {
  try {
    return await getProduct(productId);
  } catch (error) {
    if (isApiError(error) && error.code === API_ERROR_CODES.NOT_FOUND)
      return null;
    throw error;
  }
}

async function firstImageUrl(productId: string): Promise<string | null> {
  const images = await listProductImages(productId);
  return images[0]?.url ?? null;
}

/**
 * Resolves whatever `localStorage` holds against the live catalog, for the cart page to render.
 * Called directly from the cart's client component — an exported function in a `"use server"`
 * file is callable that way without a form, the same shape `features/vendor-account`'s reads
 * would take if a client component ever needed one directly.
 */
export async function resolveCartLines(
  lines: readonly CartLine[],
): Promise<CartLineDetail[]> {
  const products = await Promise.all(
    lines.map((line) => productOrNull(line.productId)),
  );

  const vendorNames = await resolveVendorNames(
    products
      .filter((product) => product !== null)
      .map((product) => product.vendorId),
  );

  const images = await Promise.all(
    products.map((product) => (product ? firstImageUrl(product.id) : null)),
  );

  return lines.map((line, index) => {
    const product = products[index];
    return {
      ...line,
      product,
      vendorName: product ? (vendorNames.get(product.vendorId) ?? null) : null,
      imageUrl: images[index],
    };
  });
}
