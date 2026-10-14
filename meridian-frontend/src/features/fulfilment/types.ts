import type { IsoInstant, Money, Uuid } from "@/shared/api";

export type DeliveryMethod = "DOOR" | "PICKUP_POINT";

export type DeliveryStatus =
  | "PENDING"
  | "BOOKED"
  | "DISPATCHED"
  | "IN_TRANSIT"
  | "DELIVERED"
  | "FAILED";

/** Mirrors `fulfilment.Carrier`. */
export interface Carrier {
  readonly id: Uuid;
  readonly name: string;
  readonly active: boolean;
}

/** Mirrors `fulfilment.DeliveryZone`. */
export interface DeliveryZone {
  readonly id: Uuid;
  readonly name: string;
  readonly county: string;
  readonly active: boolean;
}

/** Mirrors `fulfilment.DeliveryRateCard`. */
export interface DeliveryRateCard {
  readonly id: Uuid;
  readonly carrierId: Uuid;
  readonly zoneId: Uuid;
  readonly method: DeliveryMethod;
  readonly price: Money;
  readonly effectiveFrom: IsoInstant;
}

/**
 * Mirrors `fulfilment.Delivery`. One per sub-order: a parent order spanning three vendors
 * produces three deliveries, each moving through its own status.
 */
export interface Delivery {
  readonly id: Uuid;
  readonly subOrderId: Uuid;
  readonly parentOrderId: Uuid;
  readonly vendorId: Uuid;
  readonly status: DeliveryStatus;
  readonly carrierId: Uuid | null;
  readonly zoneId: Uuid | null;
  readonly method: DeliveryMethod | null;
  readonly trackingReference: string | null;
  readonly proofOfDeliveryReference: string | null;
  readonly createdAt: IsoInstant;
  readonly dispatchedAt: IsoInstant | null;
  readonly deliveredAt: IsoInstant | null;
}
