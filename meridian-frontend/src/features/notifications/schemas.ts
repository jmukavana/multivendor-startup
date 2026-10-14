import { z } from "zod";

export const marketingPreferenceSchema = z.object({
  marketingOptOut: z.boolean(),
});

export type MarketingPreferenceInput = z.infer<
  typeof marketingPreferenceSchema
>;
