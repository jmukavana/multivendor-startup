import "server-only";

import {
  api,
  apiFetchEnvelope,
  type PageRequest,
  type PaginatedResponse,
} from "@/shared/api";
import type { ParentOrder, SubOrder } from "./types";

/** The signed-in buyer's own orders. Never cached — this is one account's history. */
export async function listMyOrders({
  page = 0,
  perPage = 20,
}: PageRequest = {}): Promise<PaginatedResponse<ParentOrder>> {
  return apiFetchEnvelope<PaginatedResponse<ParentOrder>>("/orders", {
    query: { page, perPage },
  });
}

export async function getOrder(orderId: string): Promise<ParentOrder> {
  return api.get<ParentOrder>(`/orders/${orderId}`);
}

/**
 * The signed-in vendor's own order queue — the vendor-dashboard equivalent of
 * {@link listMyOrders}. FORBIDDEN if the caller owns no vendor.
 */
export async function listMyVendorSubOrders({
  page = 0,
  perPage = 20,
}: PageRequest = {}): Promise<PaginatedResponse<SubOrder>> {
  return apiFetchEnvelope<PaginatedResponse<SubOrder>>(
    "/orders/vendor/sub-orders",
    { query: { page, perPage } },
  );
}
