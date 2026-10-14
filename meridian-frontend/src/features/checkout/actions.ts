"use server";

import { refresh } from "next/cache";
import type { DeliveryZone } from "@/features/fulfilment";
import { listDeliveryZones } from "@/features/fulfilment";
import type { ParentOrder } from "@/features/orders";
import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import { toE164 } from "@/shared/lib/phone";
import { checkoutSchema } from "./schemas";

/**
 * The zones the cart page's delivery-zone select needs. A thin re-export of
 * `fulfilment.listDeliveryZones` under `requireSession` — that read is authenticated-only (see
 * its own doc), and the cart page only shows it once a buyer is signed in anyway.
 */
export async function listCheckoutZones(): Promise<DeliveryZone[]> {
  await requireSession("/cart");
  return listDeliveryZones();
}

/**
 * UC-02: turns a cart into a parent order and starts payment (BR-CHK, BR-PAY-01).
 *
 * The order comes back PENDING_PAYMENT — the M-Pesa STK push is asynchronous, and the
 * provider's callback is what confirms it. Send the buyer to the order page to wait rather
 * than treating a successful response as a paid order.
 *
 * `idempotencyKey` is the caller's, and must be one value per basket rather than per attempt:
 * that is what stops a double-submitted form from charging twice. Generate it on a route that
 * is genuinely dynamic, or a prerender will bake one shared key into every visitor's page.
 *
 * The backend rate-limits checkout per account and per source address, so a rapid retry can
 * come back RATE_LIMITED — surface that as "try again shortly", not as a payment failure.
 */
export async function checkout(
  input: unknown,
  idempotencyKey?: string,
): Promise<ActionResult<ParentOrder>> {
  await requireSession("/checkout");

  const parsed = parseInput(checkoutSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { buyerPhoneNumber, ...rest } = parsed.data;

  const result = await attempt(
    "We could not start that order. Please try again shortly.",
    () =>
      api.post<ParentOrder>(
        "/checkout",
        { ...rest, buyerPhoneNumber: toE164(buyerPhoneNumber) },
        { idempotencyKey },
      ),
  );

  if (result.status === "success") refresh();
  return result;
}
