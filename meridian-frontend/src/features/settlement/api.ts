import "server-only";

import { api } from "@/shared/api";
import type {
  SettlementLine,
  SettlementRun,
  VendorLedgerBalance,
} from "./types";

/** FINANCE_ADMIN or SUPER_ADMIN only — a 403 here means the role, not the session. */
export async function getSettlementRun(runId: string): Promise<SettlementRun> {
  return api.get<SettlementRun>(`/settlement/runs/${runId}`);
}

export async function listSettlementLines(
  runId: string,
): Promise<SettlementLine[]> {
  return api.get<SettlementLine[]>(`/settlement/runs/${runId}/lines`);
}

/**
 * A vendor's ledger position.
 *
 * The one settlement endpoint without a role guard: the backend widens the view for finance
 * admins and narrows it for the vendor themselves, so a vendor may safely read their own.
 */
export async function getVendorBalance(
  vendorId: string,
  currency = "KES",
): Promise<VendorLedgerBalance> {
  return api.get<VendorLedgerBalance>(
    `/settlement/vendors/${vendorId}/balance`,
    {
      query: { currency },
    },
  );
}
