"use server";

import { updateTag } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { CACHE_TAGS } from "@/shared/cache/tags";
import { requireSession } from "@/shared/lib/guard";
import { respondToReviewSchema, submitReviewSchema } from "./schemas";
import type { Review } from "./types";

/** A review changes both the product's public rating and its vendor's storefront rating. */
function publishReviewChange(review: Review): void {
  updateTag(CACHE_TAGS.productReviews(review.productId));
  updateTag(CACHE_TAGS.vendorReviews(review.vendorId));
}

/**
 * BR-RVW-01: only a buyer who actually took delivery of the product may review it — the
 * backend checks that, so a rejection here is expected rather than exceptional.
 */
export async function submitReview(
  input: unknown,
): Promise<ActionResult<Review>> {
  const parsed = parseInput(submitReviewSchema, input);
  if (!parsed.ok) return parsed.failure;

  await requireSession(`/products/${parsed.data.productId}`);

  const result = await attempt("We could not post that review.", () =>
    api.post<Review>("/reviews", parsed.data),
  );

  if (result.status === "success") publishReviewChange(result.data);
  return result;
}

/** BR-RVW-04: the vendor's one public reply, shown under the review. */
export async function respondToReview(
  input: unknown,
): Promise<ActionResult<Review>> {
  await requireSession("/vendor/reviews");

  const parsed = parseInput(respondToReviewSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { reviewId, response } = parsed.data;
  const result = await attempt("We could not post that response.", () =>
    api.post<Review>(`/reviews/${reviewId}/response`, { response }),
  );

  if (result.status === "success") publishReviewChange(result.data);
  return result;
}
