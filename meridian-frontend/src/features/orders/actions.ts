"use server";

import { refresh } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import { subOrderIdSchema } from "./schemas";
import type { SubOrder } from "./types";

/**
 * BR-ORD-04: the vendor takes on the sub-order, stopping the acceptance timer.
 *
 * Which of these three a caller is allowed to invoke is decided by the backend from the
 * session — a buyer confirming receipt and a vendor accepting are different people acting on
 * the same sub-order, so the frontend does not try to guess the role first.
 */
export async function acceptSubOrder(
  input: unknown,
): Promise<ActionResult<SubOrder>> {
  await requireSession("/vendor/orders");

  const parsed = parseInput(subOrderIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not accept that order.", () =>
    api.post<SubOrder>(`/orders/sub-orders/${parsed.data.subOrderId}/accept`),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-ORD-05: cancellation refunds and restocks, so it is only offered while `cancellable`. */
export async function cancelSubOrder(
  input: unknown,
): Promise<ActionResult<SubOrder>> {
  await requireSession("/orders");

  const parsed = parseInput(subOrderIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not cancel that order.", () =>
    api.post<SubOrder>(`/orders/sub-orders/${parsed.data.subOrderId}/cancel`),
  );

  if (result.status === "success") refresh();
  return result;
}

/**
 * The vendor's own manual delivery confirmation for a sub-order it is fulfilling itself,
 * outside `fulfilment`'s carrier-booked flow — see `OrderService.markDelivered`'s javadoc,
 * which requires the caller to own the vendor. (This was previously documented here as the
 * buyer confirming receipt; that did not match what the backend actually checks.)
 */
export async function markSubOrderDelivered(
  input: unknown,
): Promise<ActionResult<void>> {
  await requireSession("/vendor/orders");

  const parsed = parseInput(subOrderIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not record that delivery.", () =>
    api.post<void>(
      `/orders/sub-orders/${parsed.data.subOrderId}/mark-delivered`,
    ),
  );

  if (result.status === "success") refresh();
  return result;
}
