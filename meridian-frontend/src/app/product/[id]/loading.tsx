/** Mirrors `ProductDetailsPage`'s real layout — same grid, same column split. */
export default function ProductLoading() {
  return (
    <div className="min-h-screen bg-surface pb-28 text-on-surface">
      <div className="sticky top-0 z-40 border-b border-outline-variant bg-surface/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-[1280px] items-center gap-4 px-margin-mobile md:px-margin-desktop">
          <span className="h-9 w-9 animate-pulse rounded-full bg-surface-container-high" />
          <span className="h-7 w-28 animate-pulse rounded bg-surface-container-high" />
        </div>
      </div>

      <main className="mx-auto grid max-w-[1280px] gap-gutter px-margin-mobile py-8 md:grid-cols-12 md:px-margin-desktop">
        <section className="md:col-span-7" aria-hidden="true">
          <div className="aspect-square animate-pulse rounded-xl bg-surface-container-high" />
        </section>

        <section className="space-y-6 md:col-span-5" aria-hidden="true">
          <div className="space-y-3">
            <span className="block h-8 w-4/5 animate-pulse rounded bg-surface-container-high" />
            <span className="block h-9 w-1/3 animate-pulse rounded bg-surface-container-high" />
          </div>
          <span className="block h-16 w-full animate-pulse rounded-xl bg-surface-container-high" />
          <div className="space-y-2 pt-4">
            <span className="block h-4 w-full animate-pulse rounded bg-surface-container-high" />
            <span className="block h-4 w-full animate-pulse rounded bg-surface-container-high" />
            <span className="block h-4 w-2/3 animate-pulse rounded bg-surface-container-high" />
          </div>
        </section>
      </main>
    </div>
  );
}
