import type { Uuid } from "@/shared/api";

/**
 * Mirrors `search.internal.SortOption`. Rating is deliberately absent — see the backend
 * module's package doc for why it is not a sort key.
 */
export type ProductSortOption =
  | "RELEVANCE"
  | "PRICE_ASC"
  | "PRICE_DESC"
  | "RECENCY";

export interface ProductSearchQuery {
  readonly keyword?: string;
  readonly minPriceMinorUnits?: number;
  readonly maxPriceMinorUnits?: number;
  readonly currency?: string;
  readonly vendorId?: Uuid;
  readonly sort?: ProductSortOption;
  readonly page?: number;
  readonly perPage?: number;
}
