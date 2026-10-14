import "server-only";

import { api } from "@/shared/api";
import type { Carrier, Delivery, DeliveryZone } from "./types";

/**
 * The carriers and zones a vendor can book against.
 *
 * Reference data that changes rarely, but still authenticated — `SecurityConfiguration`
 * permits only catalog, search and the vendor rating summary unauthenticated, so these
 * travel with a bearer token and are therefore not cached.
 */
export async function listCarriers(): Promise<Carrier[]> {
  return api.get<Carrier[]>("/fulfilment/carriers");
}

export async function listDeliveryZones(): Promise<DeliveryZone[]> {
  return api.get<DeliveryZone[]>("/fulfilment/zones");
}

export async function getDelivery(deliveryId: string): Promise<Delivery> {
  return api.get<Delivery>(`/fulfilment/deliveries/${deliveryId}`);
}

/** The delivery attached to one sub-order — how a buyer tracks a single vendor's shipment. */
export async function getDeliveryForSubOrder(
  subOrderId: string,
): Promise<Delivery> {
  return api.get<Delivery>(`/fulfilment/sub-orders/${subOrderId}/delivery`);
}
