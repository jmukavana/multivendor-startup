import { z } from "zod";

/**
 * Every destructive admin action takes a reason, and the backend rejects a blank one. It ends
 * up in the audit log, which is the only durable answer to "why was this account suspended?".
 */
const reason = (label: string) =>
  z
    .string({ error: `A ${label} is required` })
    .trim()
    .min(1, { error: `A ${label} is required` })
    .max(500, { error: `The ${label} must be 500 characters or fewer` });

/** BR-PRC: a rate is a fraction of 1, exclusive of 1 — 0.15 is fifteen percent. */
const rateFraction = z
  .number({ error: "Enter a rate" })
  .min(0, { error: "A rate must be at least 0" })
  .lt(1, { error: "A rate must be less than 1" });

export const commissionRateSchema = z.object({
  rate: rateFraction,
  effectiveFrom: z.iso.datetime().optional(),
});

export const holdbackRateSchema = z.object({
  rate: rateFraction,
  effectiveFrom: z.iso.datetime().optional(),
});

export const moderateDelistSchema = z.object({
  productId: z.uuid(),
  reason: reason("reason"),
});

export const createZoneSchema = z.object({
  name: z
    .string({ error: "A zone name is required" })
    .trim()
    .min(1, { error: "A zone name is required" })
    .max(120, { error: "The name must be 120 characters or fewer" }),
  county: z
    .string({ error: "A county is required" })
    .trim()
    .min(1, { error: "A county is required" })
    .max(120, { error: "The county must be 120 characters or fewer" }),
});

export const zoneIdSchema = z.object({ zoneId: z.uuid() });

export const rateCardSchema = z.object({
  carrierId: z.uuid({ error: "Choose a carrier" }),
  zoneId: z.uuid({ error: "Choose a zone" }),
  method: z.enum(["DOOR", "PICKUP_POINT"], {
    error: "Choose a delivery method",
  }),
  priceMinorUnits: z
    .number({ error: "Enter a price" })
    .int({ error: "A price must be a whole number of cents" })
    .positive({ error: "A price must be positive" }),
  currency: z.string({ error: "A currency is required" }).regex(/^[A-Z]{3}$/, {
    error: "Use a three-letter currency code, e.g. KES",
  }),
});

export const liftRestrictionSchema = z.object({
  restrictionId: z.uuid(),
  reason: reason("reason"),
});

export const removeReviewSchema = z.object({
  reviewId: z.uuid(),
  reason: reason("removal reason"),
});

export const resolveDisputeSchema = z.object({
  disputeId: z.uuid(),
  favor: z.enum(["BUYER", "VENDOR"], { error: "Choose who this resolves for" }),
  reason: reason("resolution reason"),
});

export const applicationIdSchema = z.object({ applicationId: z.uuid() });

export const requestInformationSchema = z.object({
  applicationId: z.uuid(),
  note: reason("note"),
});

export const rejectApplicationSchema = z.object({
  applicationId: z.uuid(),
  reason: reason("rejection reason"),
});

export const suspendVendorSchema = z.object({
  vendorId: z.uuid(),
  reason: reason("suspension reason"),
});

export const impersonateVendorSchema = z.object({
  vendorId: z.uuid(),
  reason: reason("reason"),
});

export type CommissionRateInput = z.infer<typeof commissionRateSchema>;
export type HoldbackRateInput = z.infer<typeof holdbackRateSchema>;
export type CreateZoneInput = z.infer<typeof createZoneSchema>;
export type RateCardInput = z.infer<typeof rateCardSchema>;
export type ResolveDisputeInput = z.infer<typeof resolveDisputeSchema>;
export type SuspendVendorInput = z.infer<typeof suspendVendorSchema>;
