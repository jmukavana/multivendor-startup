import "server-only";

import { api } from "@/shared/api";
import type { VendorApplication } from "./types";

/**
 * Every application this account has submitted.
 *
 * The natural "do I have one yet?" read — a rejected applicant may submit again, so this is a
 * list rather than a single optional record.
 */
export async function listMyVendorApplications(): Promise<VendorApplication[]> {
  return api.get<VendorApplication[]>("/vendors/applications/mine");
}

/**
 * One application, by id.
 *
 * Scoped to the caller in the backend's WHERE clause: someone else's application id comes back
 * NOT_FOUND rather than FORBIDDEN, because confirming it exists is itself a disclosure.
 */
export async function getVendorApplication(
  applicationId: string,
): Promise<VendorApplication> {
  return api.get<VendorApplication>(`/vendors/applications/${applicationId}`);
}
