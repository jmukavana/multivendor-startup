import { z } from "zod";

import { KE_MOBILE } from "@/shared/lib/phone";

/**
 * The destination's shape depends on the method: an M-Pesa payout goes to a Kenyan mobile
 * number, a bank payout to an account number. Validating them the same way would let a
 * mistyped phone number through as a "bank account".
 */
export const changePayoutDetailSchema = z
  .object({
    vendorId: z.uuid(),
    method: z.enum(["MPESA", "BANK"], { error: "Choose a payout method" }),
    destination: z
      .string({ error: "A payout destination is required" })
      .min(1, { error: "A payout destination is required" })
      .max(64, { error: "That destination is too long" }),
  })
  .refine(
    (value) => value.method !== "MPESA" || KE_MOBILE.test(value.destination),
    {
      error: "Enter a Kenyan mobile number, e.g. 0712345678",
      path: ["destination"],
    },
  )
  .refine(
    (value) =>
      value.method !== "BANK" || /^[0-9-]{6,34}$/.test(value.destination),
    { error: "Enter a valid bank account number", path: ["destination"] },
  );

export const addStaffSchema = z.object({
  vendorId: z.uuid(),
  accountId: z.uuid({ error: "Enter the account to add" }),
});

export type ChangePayoutDetailInput = z.infer<typeof changePayoutDetailSchema>;
export type AddStaffInput = z.infer<typeof addStaffSchema>;
