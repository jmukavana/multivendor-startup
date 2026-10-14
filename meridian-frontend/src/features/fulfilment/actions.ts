"use server";

import { refresh } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import {
  bookDeliverySchema,
  deliveryIdSchema,
  manualDispatchSchema,
  markDeliveredSchema,
} from "./schemas";
import type { Delivery } from "./types";

const VENDOR_DELIVERIES_PATH = "/vendor/deliveries";

/**
 * Advances one delivery. Every transition is a POST rather than a status field a caller can
 * set, because the backend enforces the order they are allowed to happen in.
 *
 * Deliveries are read per-account and never cached, so `refresh()` — not a tag — is what puts
 * the new status on screen.
 */
async function transition(
  path: string,
  fallback: string,
  body?: unknown,
): Promise<ActionResult<Delivery>> {
  const result = await attempt(fallback, () => api.post<Delivery>(path, body));
  if (result.status === "success") refresh();
  return result;
}

/** BR-FUL-02: reserves capacity with a carrier at the rate card's price. */
export async function bookDelivery(
  input: unknown,
): Promise<ActionResult<Delivery>> {
  await requireSession(VENDOR_DELIVERIES_PATH);

  const parsed = parseInput(bookDeliverySchema, input);
  if (!parsed.ok) return parsed.failure;

  const { deliveryId, ...booking } = parsed.data;
  return transition(
    `/fulfilment/deliveries/${deliveryId}/book`,
    "We could not book that delivery.",
    booking,
  );
}

/** BR-FUL-03: hands the parcel to the booked carrier. */
export async function dispatchDelivery(
  input: unknown,
): Promise<ActionResult<Delivery>> {
  await requireSession(VENDOR_DELIVERIES_PATH);

  const parsed = parseInput(deliveryIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  return transition(
    `/fulfilment/deliveries/${parsed.data.deliveryId}/dispatch`,
    "We could not dispatch that delivery.",
  );
}

/** BR-FUL-03 A1: a vendor delivering themselves supplies their own tracking reference. */
export async function dispatchDeliveryManually(
  input: unknown,
): Promise<ActionResult<Delivery>> {
  await requireSession(VENDOR_DELIVERIES_PATH);

  const parsed = parseInput(manualDispatchSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { deliveryId, trackingReference } = parsed.data;
  return transition(
    `/fulfilment/deliveries/${deliveryId}/dispatch-manually`,
    "We could not dispatch that delivery.",
    { trackingReference },
  );
}

export async function markDeliveryInTransit(
  input: unknown,
): Promise<ActionResult<Delivery>> {
  await requireSession(VENDOR_DELIVERIES_PATH);

  const parsed = parseInput(deliveryIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  return transition(
    `/fulfilment/deliveries/${parsed.data.deliveryId}/mark-in-transit`,
    "We could not update that delivery.",
  );
}

/**
 * BR-FUL-05: completing the delivery is what starts the settlement clock, so proof of
 * delivery is worth capturing even though the backend accepts it as optional.
 */
export async function markDeliveryDelivered(
  input: unknown,
): Promise<ActionResult<Delivery>> {
  await requireSession(VENDOR_DELIVERIES_PATH);

  const parsed = parseInput(markDeliveredSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { deliveryId, proofOfDeliveryReference } = parsed.data;
  return transition(
    `/fulfilment/deliveries/${deliveryId}/mark-delivered`,
    "We could not complete that delivery.",
    { proofOfDeliveryReference: proofOfDeliveryReference ?? null },
  );
}
