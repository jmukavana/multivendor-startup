import type { IsoInstant, Uuid } from "@/shared/api";

export type VendorStatus = "ACTIVE" | "SUSPENDED";

/** Mirrors `vendor.Vendor` — what an approved application becomes. */
export interface Vendor {
  readonly id: Uuid;
  readonly ownerAccountId: Uuid;
  readonly businessName: string;
  readonly status: VendorStatus;
  readonly createdAt: IsoInstant;
}

export type PayoutMethod = "MPESA" | "BANK";

/**
 * Mirrors `PayoutDetailService.PayoutDetailView`.
 *
 * The destination itself is never returned — only whether one is on file. BR-VND-09: changing
 * payout details starts a cooling-off period during which settlement skips the vendor, which
 * is what `coolingOffUntil` reports.
 */
export interface PayoutDetailView {
  readonly onFile: boolean;
  readonly coolingOffUntil: IsoInstant | null;
}

/** Mirrors `iam.ImpersonationRecord` — BR-SEC: every admin impersonation is on the record. */
export interface ImpersonationRecord {
  readonly sessionId: Uuid;
  readonly impersonatorAccountId: Uuid;
  readonly issuedAt: IsoInstant;
  readonly expiresAt: IsoInstant;
  readonly revokedAt: IsoInstant | null;
}
