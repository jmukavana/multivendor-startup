import { z } from "zod";

import { KE_MOBILE } from "@/shared/lib/phone";

const cartLine = z.object({
  productId: z.uuid(),
  quantity: z
    .number({ error: "Enter a quantity" })
    .int({ error: "Enter a whole number" })
    .min(1, { error: "A quantity must be at least 1" }),
});

export const checkoutSchema = z.object({
  lines: z
    .array(cartLine, { error: "Your cart is empty" })
    .min(1, { error: "Your cart is empty" }),

  /** The M-Pesa number the STK push goes to — not necessarily the account's own number. */
  buyerPhoneNumber: z
    .string({ error: "A phone number is required" })
    .regex(KE_MOBILE, {
      error: "Enter a Kenyan mobile number, e.g. 0712345678",
    }),

  deliveryZoneId: z.uuid({ error: "Choose where this is going" }),

  /**
   * BR-FUL-01: delivery is chosen per vendor, because each vendor ships separately. Vendors
   * left out of the map fall back to the backend's default method for the zone.
   */
  deliveryMethodByVendor: z
    .record(z.uuid(), z.enum(["DOOR", "PICKUP_POINT"]))
    .optional(),
});

export type CheckoutInput = z.infer<typeof checkoutSchema>;
