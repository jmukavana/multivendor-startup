import type { IsoInstant, Uuid } from "@/shared/api";

/**
 * Mirrors `InventoryController.StockView`. `availableQuantity` is what is left after live
 * reservations, so it trails `totalQuantity` whenever carts are open.
 */
export interface StockView {
  readonly totalQuantity: number;
  readonly availableQuantity: number;
}

/** Mirrors `StockMovementEntity.Reason`. */
export type StockMovementReason =
  | "RESTOCK"
  | "RESERVED"
  | "RELEASED"
  | "CONFIRMED"
  | "EXPIRED"
  | "CANCELLED";

/** AC-INV-04: every movement, with the transaction that caused it. */
export interface StockMovement {
  readonly quantityDelta: number;
  readonly reason: StockMovementReason;
  readonly referenceId: Uuid | null;
  readonly occurredAt: IsoInstant;
}
