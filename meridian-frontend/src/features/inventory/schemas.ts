import { z } from "zod";

export const setStockSchema = z.object({
  productId: z.uuid(),
  totalQuantity: z
    .number({ error: "Enter a quantity" })
    .int({ error: "Enter a whole number" })
    .min(0, { error: "A quantity must not be negative" }),
});

export type SetStockInput = z.infer<typeof setStockSchema>;
