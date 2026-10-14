import type { IsoInstant, Money, Uuid } from "@/shared/api";

/** Mirrors `catalog.ProductStatus`. Only ACTIVE products are orderable. */
export type ProductStatus = "DRAFT" | "ACTIVE" | "DELISTED";

/** Mirrors `catalog.Product`. */
export interface Product {
  readonly id: Uuid;
  readonly vendorId: Uuid;
  readonly name: string;
  readonly description: string | null;
  readonly price: Money;
  readonly status: ProductStatus;
  readonly createdAt: IsoInstant;
}

/** Mirrors `ProductPriceChangeEntity.PriceChangeView` — AC-CAT-06's audit trail. */
export interface PriceChange {
  readonly previousPrice: Money;
  readonly newPrice: Money;
  readonly actorAccountId: Uuid;
  readonly changedAt: IsoInstant;
}

/**
 * Mirrors `CatalogController.ImageView`. `url` is a path this app's own origin proxies — see
 * `getProduct` — never the opaque blob reference itself.
 */
export interface ProductImage {
  readonly id: Uuid;
  readonly contentType: string;
  readonly sortOrder: number;
  readonly url: string;
}
