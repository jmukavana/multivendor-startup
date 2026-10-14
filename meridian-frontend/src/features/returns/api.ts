import "server-only";

import { api } from "@/shared/api";
import type { Return } from "./types";

/** The signed-in buyer's return requests. */
export async function listMyReturns(): Promise<Return[]> {
  return api.get<Return[]>("/returns/mine");
}

/** Readable by either side of the return — the backend decides which the caller is. */
export async function getReturn(returnId: string): Promise<Return> {
  return api.get<Return>(`/returns/${returnId}`);
}
