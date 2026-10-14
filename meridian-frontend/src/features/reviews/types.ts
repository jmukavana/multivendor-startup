import type { IsoInstant, Uuid } from "@/shared/api";

export type ReviewStatus = "ACTIVE" | "REMOVED";

/** Mirrors `review.Review`. */
export interface Review {
  readonly id: Uuid;
  readonly productId: Uuid;
  readonly vendorId: Uuid;
  readonly buyerAccountId: Uuid;
  readonly rating: number;
  readonly comment: string | null;
  readonly vendorResponse: string | null;
  readonly status: ReviewStatus;
  readonly removalReason: string | null;
  readonly createdAt: IsoInstant;
}

/** Mirrors `review.RatingSummary`. `averageRating` is a JSON number, not a string. */
export interface RatingSummary {
  readonly averageRating: number;
  readonly reviewCount: number;
}
