import type { Product } from "@/features/catalog";
import type { Uuid } from "@/shared/api";

/**
 * The cart's own shape — exactly what `/checkout` needs per line (see
 * `features/checkout/schemas.ts`) and nothing resolved yet. This, not a `Product`, is what
 * lives in `localStorage`: a product's price or availability can change while it sits in
 * someone's cart, so the cart holds only the id and quantity and re-resolves the rest fresh
 * whenever it is shown.
 */
export interface CartLine {
  readonly productId: Uuid;
  readonly quantity: number;
}

/**
 * One cart line resolved against the live catalog. `product` is `null` when the listing has
 * since been delisted or removed — a real, current state a cart display has to be honest
 * about, not a lookup failure to hide.
 */
export interface CartLineDetail extends CartLine {
  readonly product: Product | null;
  readonly vendorName: string | null;
  readonly imageUrl: string | null;
}
