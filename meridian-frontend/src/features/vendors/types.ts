import type { Uuid } from "@/shared/api";

/**
 * Mirrors `VendorDirectoryController.VendorSummary` — the public half of a vendor's identity, a
 * business name to show next to its listings. Nothing else: payout state, ownership, staff and
 * status live in `features/vendor-account`, which is about a vendor managing itself, not a
 * buyer browsing what it sells.
 */
export interface VendorSummary {
  readonly id: Uuid;
  readonly businessName: string;
}
