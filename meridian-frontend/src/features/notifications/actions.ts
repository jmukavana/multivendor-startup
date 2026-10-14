"use server";

import { refresh } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import { marketingPreferenceSchema } from "./schemas";
import type { MarketingPreference } from "./types";

export async function setMarketingPreference(
  input: unknown,
): Promise<ActionResult<MarketingPreference>> {
  await requireSession("/account/notifications");

  const parsed = parseInput(marketingPreferenceSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not save that preference.", () =>
    api.put<MarketingPreference>("/notifications/preferences", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}
