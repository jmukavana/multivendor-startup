import type { IsoInstant, Money, Uuid } from "@/shared/api";

export type PaymentStatus =
  | "INITIATED"
  | "AWAITING_CONFIRMATION"
  | "CONFIRMED"
  | "FAILED"
  | "TIMED_OUT"
  | "REVERSED";

/** Mirrors `payment.PaymentTransaction`. */
export interface PaymentTransaction {
  readonly id: Uuid;
  readonly parentOrderId: Uuid;
  readonly amount: Money;
  readonly status: PaymentStatus;
  readonly providerTransactionReference: string | null;
  readonly initiatedAt: IsoInstant;
}

export type RefundStatus = "INITIATED" | "CONFIRMED" | "FAILED";

/** Mirrors `payment.Refund`. Refunds are raised by the backend, never by a frontend call. */
export interface Refund {
  readonly id: Uuid;
  readonly paymentTransactionId: Uuid;
  readonly amount: Money;
  readonly reason: string;
  readonly status: RefundStatus;
  readonly providerRefundReference: string | null;
  readonly initiatedAt: IsoInstant;
}
