import type { IsoInstant, Money, Uuid } from "@/shared/api";

export type ReturnReason =
  | "DAMAGED"
  | "WRONG_ITEM"
  | "NOT_AS_DESCRIBED"
  | "DEFECTIVE"
  | "CHANGED_MIND"
  | "OTHER";

export type ReturnStatus =
  | "REQUESTED"
  | "ESCALATED"
  | "RESOLVED_BUYER"
  | "RESOLVED_VENDOR";

/**
 * Mirrors `returns.Return`.
 *
 * `refundAmount` is computed by the backend from the sub-order's actual purchased lines, never
 * taken from the request — a buyer-supplied figure would be an obvious abuse vector (RSK-08).
 */
export interface Return {
  readonly id: Uuid;
  readonly subOrderId: Uuid;
  readonly parentOrderId: Uuid;
  readonly buyerAccountId: Uuid;
  readonly vendorId: Uuid;
  readonly reason: ReturnReason;
  readonly description: string | null;
  readonly imageReferences: readonly string[];
  readonly refundAmount: Money;
  readonly status: ReturnStatus;
  readonly requestedAt: IsoInstant;
  readonly resolvedAt: IsoInstant | null;
}

export type DisputeFavor = "BUYER" | "VENDOR";

export type DisputeStatus = "OPEN" | "RESOLVED";

/** Mirrors `returns.Dispute` — raised when a vendor refuses a return. */
export interface Dispute {
  readonly id: Uuid;
  readonly returnId: Uuid;
  readonly status: DisputeStatus;
  readonly resolutionFavor: DisputeFavor | null;
  readonly resolutionReason: string | null;
  readonly resolvedByAccountId: Uuid | null;
  readonly createdAt: IsoInstant;
  readonly resolvedAt: IsoInstant | null;
}
