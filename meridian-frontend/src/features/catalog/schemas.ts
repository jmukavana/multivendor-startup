import { z } from "zod";

const currency = z
  .string({ error: "A currency is required" })
  .regex(/^[A-Z]{3}$/, { error: "Use a three-letter currency code, e.g. KES" });

const priceMinorUnits = z
  .number({ error: "A price is required" })
  .int({ error: "A price must be a whole number of cents" })
  .min(0, { error: "A price must not be negative" });

export const createProductSchema = z.object({
  name: z
    .string({ error: "A product name is required" })
    .min(2, { error: "The product name is too short" })
    .max(200, { error: "The product name must be 200 characters or fewer" }),
  description: z
    .string()
    .max(4000, { error: "The description must be 4000 characters or fewer" })
    .optional(),
  priceMinorUnits,
  currency,
});

export const changePriceSchema = z.object({
  productId: z.uuid(),
  priceMinorUnits,
  currency,
});

export const productIdSchema = z.object({ productId: z.uuid() });

export type CreateProductInput = z.infer<typeof createProductSchema>;
export type ChangePriceInput = z.infer<typeof changePriceSchema>;
