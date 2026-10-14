import { z } from "zod";

export const requestReturnSchema = z.object({
  subOrderId: z.uuid(),
  reason: z.enum(
    [
      "DAMAGED",
      "WRONG_ITEM",
      "NOT_AS_DESCRIBED",
      "DEFECTIVE",
      "CHANGED_MIND",
      "OTHER",
    ],
    { error: "Choose a reason" },
  ),
  description: z
    .string()
    .max(2000, { error: "The description must be 2000 characters or fewer" })
    .optional(),
  lines: z
    .array(
      z.object({
        productId: z.uuid(),
        quantity: z
          .number({ error: "Enter a quantity" })
          .int({ error: "Enter a whole number" })
          .min(1, { error: "A quantity must be at least 1" }),
      }),
      { error: "Choose at least one item to return" },
    )
    .min(1, { error: "Choose at least one item to return" }),
});

export const vendorResponseSchema = z.object({
  returnId: z.uuid(),
  accept: z.boolean(),
});

export const returnIdSchema = z.object({ returnId: z.uuid() });

export type RequestReturnInput = z.infer<typeof requestReturnSchema>;
export type VendorResponseInput = z.infer<typeof vendorResponseSchema>;
