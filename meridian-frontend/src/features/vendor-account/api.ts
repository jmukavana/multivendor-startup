import "server-only";

import { api } from "@/shared/api";
import type { ImpersonationRecord, PayoutDetailView } from "./types";

/**
 * Whether payout details are on file, and whether the vendor is in cooling-off.
 *
 * A caller unrelated to the vendor gets NOT_FOUND rather than FORBIDDEN — the backend will
 * not confirm that a vendor id exists to someone with no business knowing.
 */
export async function getPayoutDetails(
  vendorId: string,
): Promise<PayoutDetailView> {
  return api.get<PayoutDetailView>(`/vendors/${vendorId}/payout-details`);
}

/** BR-SEC: the vendor's own view of who has signed in as them, and when. */
export async function listImpersonations(
  vendorId: string,
): Promise<ImpersonationRecord[]> {
  return api.get<ImpersonationRecord[]>(
    `/vendors/${vendorId}/impersonation-log`,
  );
}
