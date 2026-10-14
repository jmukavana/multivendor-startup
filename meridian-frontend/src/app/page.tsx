import Link from "next/link";

import { listProductImages, listProducts } from "@/features/catalog";
import { getProductRatingSummary } from "@/features/reviews";
import { type ProductSortOption, searchProducts } from "@/features/search";
import { resolveVendorNames } from "@/features/vendors";
import { ProductTile } from "@/shared/components/marketplace/ProductTile";
import {
  MarketplaceHeader,
  MobileNav,
} from "@/shared/components/marketplace/Shell";
import { Button } from "@/shared/components/ui/button";

const SORTS: ReadonlyArray<{
  readonly value: ProductSortOption | null;
  readonly label: string;
}> = [
  { value: null, label: "Featured" },
  { value: "RECENCY", label: "Newest" },
  { value: "PRICE_ASC", label: "Price: Low to High" },
  { value: "PRICE_DESC", label: "Price: High to Low" },
];

function isSortOption(value: string | undefined): value is ProductSortOption {
  return (
    value === "RECENCY" ||
    value === "PRICE_ASC" ||
    value === "PRICE_DESC" ||
    value === "RELEVANCE"
  );
}

function sortHref(sort: ProductSortOption | null, q: string): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (sort) params.set("sort", sort);
  const query = params.toString();
  return query ? `/?${query}` : "/";
}

export default async function HomePage({ searchParams }: PageProps<"/">) {
  const params = await searchParams;
  const q = typeof params.q === "string" ? params.q.trim() : "";
  const sortParam = typeof params.sort === "string" ? params.sort : undefined;
  const page = Number(params.page) || 0;

  // A bare "/" uses the plain, publicly cached listing — the fastest path, and correct for the
  // overwhelming majority of visits. Search only enters once there is a keyword or an explicit
  // sort to honour, which `listProducts` (BR-SCH-04's recency order only) cannot express.
  const usingSearch = q.length > 0 || isSortOption(sortParam);
  const sort: ProductSortOption = isSortOption(sortParam)
    ? sortParam
    : "RELEVANCE";

  const result = usingSearch
    ? await searchProducts({ keyword: q || undefined, sort, page })
    : await listProducts({ page });

  const products = result.data;

  const [vendorNames, ratings, images] = await Promise.all([
    resolveVendorNames(products.map((product) => product.vendorId)),
    Promise.all(
      products.map((product) =>
        getProductRatingSummary(product.id).catch(() => null),
      ),
    ),
    Promise.all(
      products.map((product) =>
        listProductImages(product.id).then((list) => list[0]?.url ?? null),
      ),
    ),
  ]);

  const { totalPages, page: currentPage } = result.meta.pagination;

  return (
    <div className="min-h-screen bg-surface pb-24 text-on-surface md:pb-0">
      <MarketplaceHeader active="home" />

      <main className="mx-auto max-w-[1280px] px-margin-mobile py-8 md:px-margin-desktop">
        <section aria-label="Sort" className="mb-12 md:mb-14">
          <div className="-mx-margin-mobile flex gap-3 overflow-x-auto px-margin-mobile pb-2 md:mx-0 md:px-0">
            {SORTS.map((option) => {
              const active = usingSearch
                ? option.value === sortParam
                : option.value === null;
              return (
                <Button
                  key={option.label}
                  variant={active ? "default" : "outline"}
                  className="h-11 shrink-0 rounded-full px-5"
                  asChild
                >
                  <Link href={sortHref(option.value, q)}>{option.label}</Link>
                </Button>
              );
            })}
          </div>
        </section>

        <section aria-labelledby="recommended-heading">
          <div className="mb-6 flex items-end justify-between gap-4">
            <h1
              id="recommended-heading"
              className="text-2xl font-bold leading-8 text-on-surface md:text-[32px] md:leading-10"
            >
              {q ? `Results for "${q}"` : "Recommended for You"}
            </h1>
          </div>

          {products.length === 0 ? (
            <p className="py-16 text-center text-body-md text-on-surface-variant">
              {q
                ? "No products matched your search."
                : "No products are listed yet."}
            </p>
          ) : (
            <div className="grid grid-cols-2 gap-5 md:grid-cols-4 md:gap-gutter lg:grid-cols-5">
              {products.map((product, index) => (
                <ProductTile
                  key={product.id}
                  product={product}
                  vendorName={vendorNames.get(product.vendorId) ?? null}
                  imageUrl={images[index]}
                  rating={ratings[index]}
                />
              ))}
            </div>
          )}

          {totalPages > 1 && (
            <nav
              className="mt-10 flex items-center justify-center gap-3"
              aria-label="Pagination"
            >
              {currentPage > 1 && (
                <Button variant="outline" asChild>
                  <Link href={pageHref(sort, q, usingSearch, page - 1)}>
                    Previous
                  </Link>
                </Button>
              )}
              <span className="text-label-md text-on-surface-variant">
                Page {currentPage} of {totalPages}
              </span>
              {currentPage < totalPages && (
                <Button variant="outline" asChild>
                  <Link href={pageHref(sort, q, usingSearch, page + 1)}>
                    Next
                  </Link>
                </Button>
              )}
            </nav>
          )}
        </section>
      </main>

      <MobileNav active="home" />
    </div>
  );
}

function pageHref(
  sort: ProductSortOption,
  q: string,
  usingSearch: boolean,
  page: number,
): string {
  const params = new URLSearchParams();
  if (q) params.set("q", q);
  if (usingSearch) params.set("sort", sort);
  params.set("page", String(page));
  return `/?${params.toString()}`;
}
