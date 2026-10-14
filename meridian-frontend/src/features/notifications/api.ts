import "server-only";

import { api } from "@/shared/api";
import type { MarketingPreference } from "./types";

export async function getMarketingPreference(): Promise<MarketingPreference> {
  return api.get<MarketingPreference>("/notifications/preferences");
}
