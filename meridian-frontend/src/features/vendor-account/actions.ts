"use server";

import { refresh } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import { toE164 } from "@/shared/lib/phone";
import { addStaffSchema, changePayoutDetailSchema } from "./schemas";

/**
 * BR-VND-09: changes where this vendor's money goes.
 *
 * The change takes effect immediately but starts a cooling-off period during which settlement
 * skips the vendor — a stolen session redirecting payouts cannot also cash them out before
 * anyone notices. Say so in the UI: a vendor who is not told will read the pause as a bug.
 */
export async function changePayoutDetails(
  input: unknown,
): Promise<ActionResult<void>> {
  const parsed = parseInput(changePayoutDetailSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { vendorId, method, destination } = parsed.data;
  await requireSession(`/vendor/${vendorId}/payouts`);

  const result = await attempt("We could not save those payout details.", () =>
    api.put<void>(`/vendors/${vendorId}/payout-details`, {
      method,
      destination: method === "MPESA" ? toE164(destination) : destination,
    }),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-VND-08: only the owner may add staff — the backend enforces that, not this call. */
export async function addVendorStaff(
  input: unknown,
): Promise<ActionResult<void>> {
  const parsed = parseInput(addStaffSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { vendorId, accountId } = parsed.data;
  await requireSession(`/vendor/${vendorId}/staff`);

  const result = await attempt("We could not add that staff member.", () =>
    api.post<void>(`/vendors/${vendorId}/staff`, { accountId }),
  );

  if (result.status === "success") refresh();
  return result;
}
