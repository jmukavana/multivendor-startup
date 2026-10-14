import { z } from "zod";

const deliveryMethod = z.enum(["DOOR", "PICKUP_POINT"], {
  error: "Choose a delivery method",
});

export const bookDeliverySchema = z.object({
  deliveryId: z.uuid(),
  carrierId: z.uuid({ error: "Choose a carrier" }),
  zoneId: z.uuid({ error: "Choose a delivery zone" }),
  method: deliveryMethod,
});

export const deliveryIdSchema = z.object({ deliveryId: z.uuid() });

export const manualDispatchSchema = z.object({
  deliveryId: z.uuid(),
  trackingReference: z
    .string({ error: "A tracking reference is required" })
    .min(1, { error: "A tracking reference is required" })
    .max(120, { error: "The tracking reference is too long" }),
});

export const markDeliveredSchema = z.object({
  deliveryId: z.uuid(),
  proofOfDeliveryReference: z.string().max(200).optional(),
});

export type BookDeliveryInput = z.infer<typeof bookDeliverySchema>;
export type ManualDispatchInput = z.infer<typeof manualDispatchSchema>;
