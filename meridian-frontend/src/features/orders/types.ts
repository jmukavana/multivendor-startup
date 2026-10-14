import type { IsoInstant, Money, Uuid } from "@/shared/api";

export type OrderStatus =
  | "PENDING_PAYMENT"
  | "CONFIRMED"
  | "ACCEPTED"
  | "DELIVERED"
  | "CANCELLED";

/** Mirrors `order.OrderLine`. The product name is snapshotted at purchase, not looked up. */
export interface OrderLine {
  readonly productId: Uuid;
  readonly productNameSnapshot: string;
  readonly unitPrice: Money;
  readonly quantity: number;
  readonly lineTotal: Money;
}

/**
 * Mirrors `order.SubOrder` — one vendor's share of a basket. A buyer pays once, but accepts,
 * cancels and takes delivery per sub-order, because each is a different vendor's obligation.
 */
export interface SubOrder {
  readonly id: Uuid;
  readonly vendorId: Uuid;
  readonly status: OrderStatus;
  readonly subtotal: Money;
  readonly deliveryCost: Money;
  readonly lines: readonly OrderLine[];
  readonly commissionConfigurationId: Uuid;
  readonly holdbackConfigurationId: Uuid;
  readonly cancellable: boolean;
  /** BR-ORD-04: past this, an unaccepted sub-order is auto-cancelled. */
  readonly acceptanceDeadline: IsoInstant | null;
  readonly createdAt: IsoInstant;
}

/** Mirrors `order.ParentOrder` — what the buyer sees as "an order". */
export interface ParentOrder {
  readonly id: Uuid;
  readonly reference: string;
  readonly buyerAccountId: Uuid;
  readonly status: OrderStatus;
  readonly total: Money;
  readonly subOrders: readonly SubOrder[];
  readonly createdAt: IsoInstant;
  readonly sourceAddress: string | null;
  readonly userAgent: string | null;
}

/** `SubOrder.total()` is a Java method, not a serialised field — recompute it here. */
export function subOrderTotal(subOrder: SubOrder): Money {
  return {
    minorUnits: subOrder.subtotal.minorUnits + subOrder.deliveryCost.minorUnits,
    currency: subOrder.subtotal.currency,
  };
}
