import { BadgeCheck, Package, Star } from "lucide-react";
import Image from "next/image";
import Link from "next/link";

import type { Product } from "@/features/catalog";
import type { RatingSummary } from "@/features/reviews";
import { Card, CardContent } from "@/shared/components/ui/card";
import { formatMoney } from "@/shared/lib/money";
import { AddToCartButton } from "./AddToCartButton";

interface ProductTileProps {
  readonly product: Product;
  /** `null` when {@link VendorSummary} lookup did not resolve it — shown without the byline. */
  readonly vendorName: string | null;
  /** `null` when the listing has no images yet — shown with a neutral placeholder, not a stock photo. */
  readonly imageUrl: string | null;
  /** `null` when nobody has reviewed it yet — the rating row is omitted rather than showing 0. */
  readonly rating: RatingSummary | null;
}

export function ProductTile({
  product,
  vendorName,
  imageUrl,
  rating,
}: ProductTileProps) {
  return (
    <Card className="product-card relative overflow-hidden rounded-xl border-outline-variant/50 shadow-[0px_4px_20px_rgba(0,0,0,0.08)]">
      <Link
        href={`/product/${product.id}`}
        className="relative block aspect-square overflow-hidden bg-surface-container-low"
        aria-label={product.name}
      >
        {imageUrl ? (
          <Image
            src={imageUrl}
            alt=""
            fill
            sizes="(max-width: 768px) 50vw, (max-width: 1024px) 25vw, 20vw"
            className="object-cover"
          />
        ) : (
          <div className="flex h-full w-full items-center justify-center text-on-surface-variant/40">
            <Package className="h-10 w-10" />
          </div>
        )}
      </Link>

      <CardContent className="flex min-h-[180px] flex-col p-4">
        {rating && rating.reviewCount > 0 && (
          <div className="mb-2 flex items-center gap-1.5 text-label-sm text-on-surface-variant">
            <Star className="h-3.5 w-3.5 fill-secondary-fixed-dim text-secondary-fixed-dim" />
            <span>
              {rating.averageRating.toFixed(1)} ({rating.reviewCount})
            </span>
          </div>
        )}

        <h2 className="mb-2 line-clamp-2 min-h-10 text-base font-semibold leading-5 text-on-surface md:text-label-md">
          <Link href={`/product/${product.id}`} className="hover:text-primary">
            {product.name}
          </Link>
        </h2>

        {vendorName && (
          <div className="mb-4 flex min-w-0 items-center gap-1.5 text-sm text-on-surface-variant">
            <BadgeCheck className="h-3.5 w-3.5 shrink-0 text-secondary" />
            <span className="truncate">{vendorName}</span>
          </div>
        )}

        <div className="mt-auto flex items-end justify-between gap-3">
          <p className="min-w-0 font-bold text-on-surface">
            {formatMoney(product.price)}
          </p>
          <AddToCartButton productId={product.id} productName={product.name} />
        </div>
      </CardContent>
    </Card>
  );
}

export { MarketplaceHeader, MobileNav } from "./Shell";
