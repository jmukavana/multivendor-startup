import type { IsoInstant, Money, Uuid } from "@/shared/api";

export type SettlementRunStatus =
  | "BLOCKED"
  | "DRAFT"
  | "APPROVED"
  | "COMPLETED";

export type SettlementLineStatus =
  | "PENDING"
  | "EXCLUDED"
  | "DISBURSING"
  | "DISBURSED"
  | "FAILED"
  | "ESCALATED";

/** Mirrors `settlement.SettlementRun`. `currency` is the ISO-4217 code, e.g. "KES". */
export interface SettlementRun {
  readonly id: Uuid;
  readonly currency: string;
  readonly status: SettlementRunStatus;
  readonly createdAt: IsoInstant;
  readonly approvedByAccountId: Uuid | null;
  readonly approvedAt: IsoInstant | null;
}

/** Mirrors `settlement.SettlementLine` — one vendor's payout inside a run. */
export interface SettlementLine {
  readonly id: Uuid;
  readonly settlementRunId: Uuid;
  readonly vendorId: Uuid;
  readonly amount: Money;
  readonly status: SettlementLineStatus;
  /** Why this vendor was left out — a payout hold, missing payout details, cooling-off. */
  readonly exclusionReason: string | null;
  readonly attemptCount: number;
  readonly disbursementReference: string | null;
}

/** Mirrors `settlement.SettlementCycle`. */
export interface SettlementCycle {
  readonly configurationVersionId: Uuid;
  readonly cycleDays: number;
}

/**
 * Mirrors `ledger.VendorLedgerBalance`. `payable` is releasable now; `holdback` is retained
 * against returns and is released on its own schedule.
 */
export interface VendorLedgerBalance {
  readonly vendorId: Uuid;
  readonly payable: Money;
  readonly holdback: Money;
}
