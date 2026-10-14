"use server";

import { refresh } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import { setStockSchema } from "./schemas";
import type { StockView } from "./types";

/**
 * BR-INV-02: sets the absolute on-hand total, not a delta.
 *
 * The response's `availableQuantity` is recomputed after the write, so it can come back lower
 * than the total that was just set — live reservations are already holding some of it.
 */
export async function setStock(
  input: unknown,
): Promise<ActionResult<StockView>> {
  await requireSession("/vendor/inventory");

  const parsed = parseInput(setStockSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { productId, totalQuantity } = parsed.data;
  const result = await attempt("We could not update that stock level.", () =>
    api.put<StockView>(`/inventory/products/${productId}/stock`, {
      totalQuantity,
    }),
  );

  // Stock is a per-vendor read and never cached, so there is no tag to clear — re-rendering
  // the page is what shows the new number.
  if (result.status === "success") refresh();
  return result;
}
