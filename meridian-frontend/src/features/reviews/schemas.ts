import { z } from "zod";

export const submitReviewSchema = z.object({
  productId: z.uuid(),
  rating: z
    .number({ error: "Choose a rating" })
    .int({ error: "Choose a rating" })
    .min(1, { error: "Rate the product between 1 and 5" })
    .max(5, { error: "Rate the product between 1 and 5" }),
  comment: z
    .string()
    .max(2000, { error: "The comment must be 2000 characters or fewer" })
    .optional(),
});

export const respondToReviewSchema = z.object({
  reviewId: z.uuid(),
  response: z
    .string({ error: "Write a response" })
    .min(1, { error: "Write a response" })
    .max(2000, { error: "The response must be 2000 characters or fewer" }),
});

export type SubmitReviewInput = z.infer<typeof submitReviewSchema>;
export type RespondToReviewInput = z.infer<typeof respondToReviewSchema>;
