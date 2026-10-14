import { BadgeCheck, Package, Star } from "lucide-react";
import Image from "next/image";
import { notFound } from "next/navigation";

import {
  getProduct,
  listProductImages,
  type Product,
} from "@/features/catalog";
import {
  getProductRatingSummary,
  listProductReviews,
} from "@/features/reviews";
import { resolveVendorNames } from "@/features/vendors";
import { API_ERROR_CODES, isApiError } from "@/shared/api";
import { ProductPurchasePanel } from "@/shared/components/marketplace/ProductPurchasePanel";
import { MarketplaceHeader } from "@/shared/components/marketplace/Shell";
import { Card, CardContent } from "@/shared/components/ui/card";
import { formatMoney } from "@/shared/lib/money";

const STAR_POSITIONS = [1, 2, 3, 4, 5] as const;

async function loadProduct(productId: string): Promise<Product> {
  try {
    return await getProduct(productId);
  } catch (error) {
    if (isApiError(error) && error.code === API_ERROR_CODES.NOT_FOUND)
      notFound();
    throw error;
  }
}

export async function generateMetadata({ params }: PageProps<"/product/[id]">) {
  const { id } = await params;
  try {
    const product = await getProduct(id);
    return { title: `${product.name} — Meridian` };
  } catch {
    return { title: "Meridian" };
  }
}

export default async function ProductDetailsPage({
  params,
}: PageProps<"/product/[id]">) {
  const { id } = await params;
  const product = await loadProduct(id);

  const [images, vendorNames, rating, reviews] = await Promise.all([
    listProductImages(product.id),
    resolveVendorNames([product.vendorId]),
    getProductRatingSummary(product.id).catch(() => null),
    listProductReviews(product.id).catch(() => []),
  ]);

  const vendorName = vendorNames.get(product.vendorId) ?? "Unknown vendor";
  const [primaryImage, ...secondaryImages] = images;

  return (
    <div className="min-h-screen bg-surface pb-28 text-on-surface">
      <MarketplaceHeader backHref="/" title="Meridian" />

      <main className="mx-auto grid max-w-[1280px] gap-gutter px-margin-mobile py-8 md:grid-cols-12 md:px-margin-desktop">
        <section className="space-y-4 md:col-span-7" aria-label="Product media">
          <Card className="relative aspect-square overflow-hidden rounded-xl">
            {primaryImage ? (
              <Image
                src={primaryImage.url}
                alt=""
                fill
                priority
                sizes="(max-width: 768px) 100vw, 58vw"
                className="object-cover"
              />
            ) : (
              <div className="flex h-full w-full items-center justify-center text-on-surface-variant/40">
                <Package className="h-16 w-16" />
              </div>
            )}
          </Card>

          {secondaryImages.length > 0 && (
            <div className="grid grid-cols-4 gap-4">
              {secondaryImages.map((image) => (
                <div
                  key={image.id}
                  className="relative aspect-square overflow-hidden rounded-lg bg-surface-container-low shadow-[0px_4px_20px_rgba(0,0,0,0.08)]"
                >
                  <Image
                    src={image.url}
                    alt=""
                    fill
                    sizes="25vw"
                    className="object-cover"
                  />
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="space-y-6 md:col-span-5">
          <div>
            <h1 className="mb-2 text-2xl font-bold leading-8 md:text-[32px] md:leading-10">
              {product.name}
            </h1>
            <p className="text-[32px] font-bold leading-10 text-primary">
              {formatMoney(product.price)}
            </p>
          </div>

          <Card>
            <CardContent className="flex items-center gap-4 p-4">
              <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary-container text-label-md font-bold text-on-primary-container">
                {vendorName.slice(0, 2).toUpperCase()}
              </div>
              <div>
                <p className="flex items-center gap-1 text-label-md font-semibold">
                  {vendorName}
                  <BadgeCheck className="h-4 w-4 text-secondary" />
                </p>
                {rating && rating.reviewCount > 0 && (
                  <p className="flex items-center gap-1 text-label-sm text-on-surface-variant">
                    <Star className="h-3.5 w-3.5 fill-secondary-fixed-dim text-secondary-fixed-dim" />
                    {rating.averageRating.toFixed(1)} ({rating.reviewCount}{" "}
                    reviews)
                  </p>
                )}
              </div>
            </CardContent>
          </Card>

          {product.description && (
            <div className="border-outline-variant border-t pt-6">
              <h2 className="mb-3 text-title-md font-semibold">
                Product Description
              </h2>
              <p className="whitespace-pre-line text-body-md text-on-surface-variant">
                {product.description}
              </p>
            </div>
          )}

          {reviews.length > 0 && (
            <div className="border-outline-variant border-t pt-6">
              <h2 className="mb-3 text-title-md font-semibold">
                Reviews ({reviews.length})
              </h2>
              <ul className="space-y-4">
                {reviews.slice(0, 3).map((review) => (
                  <li key={review.id} className="text-body-md">
                    <div className="mb-1 flex items-center gap-1 text-secondary-fixed-dim">
                      {STAR_POSITIONS.map((position) => (
                        <Star
                          key={position}
                          className={`h-3.5 w-3.5 ${position <= review.rating ? "fill-current" : "text-outline-variant"}`}
                        />
                      ))}
                    </div>
                    {review.comment && (
                      <p className="text-on-surface-variant">
                        {review.comment}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </section>
      </main>

      <ProductPurchasePanel productId={product.id} />
    </div>
  );
}
