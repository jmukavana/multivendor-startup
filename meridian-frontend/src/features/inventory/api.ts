import "server-only";

import { api } from "@/shared/api";
import type { StockMovement, StockView } from "./types";

/** BR-INV-01: the vendor's own stock position for one listing. */
export async function getStock(productId: string): Promise<StockView> {
  return api.get<StockView>(`/inventory/products/${productId}/stock`);
}

export async function listStockMovements(
  productId: string,
): Promise<StockMovement[]> {
  return api.get<StockMovement[]>(`/inventory/products/${productId}/movements`);
}
