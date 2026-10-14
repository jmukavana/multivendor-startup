import { z } from "zod";

export const initiateRunSchema = z.object({
  currency: z.string({ error: "A currency is required" }).regex(/^[A-Z]{3}$/, {
    error: "Use a three-letter currency code, e.g. KES",
  }),
});

export const runIdSchema = z.object({ runId: z.uuid() });

export const holdVendorSchema = z.object({
  vendorId: z.uuid(),
  reason: z
    .string({ error: "A hold reason is required" })
    .min(1, { error: "A hold reason is required" })
    .max(500, { error: "The reason must be 500 characters or fewer" }),
});

export const releaseHoldSchema = z.object({ holdId: z.uuid() });

export const settlementCycleSchema = z.object({
  cycleDays: z
    .number({ error: "Enter a cycle length" })
    .int({ error: "Enter a whole number of days" })
    .min(1, { error: "A cycle must be at least 1 day" })
    .max(31, { error: "A cycle must be at most 31 days" }),
  /** Omitted means "from now" — the backend stamps its own clock rather than trusting ours. */
  effectiveFrom: z.iso.datetime().optional(),
});

export type InitiateRunInput = z.infer<typeof initiateRunSchema>;
export type HoldVendorInput = z.infer<typeof holdVendorSchema>;
export type SettlementCycleInput = z.infer<typeof settlementCycleSchema>;
