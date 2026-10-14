/** Mirrors `CartPage`'s real layout — the item list plus the summary rail beside it. */
export default function CartLoading() {
  return (
    <div className="min-h-screen bg-background pb-24 text-on-background md:pb-0">
      <div className="sticky top-0 z-40 border-b border-outline-variant bg-surface/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-[1280px] items-center gap-4 px-margin-mobile md:px-margin-desktop">
          <span className="h-7 w-28 animate-pulse rounded bg-surface-container-high" />
        </div>
      </div>

      <main className="mx-auto flex w-full max-w-[1280px] flex-col gap-gutter px-margin-mobile py-8 md:px-margin-desktop md:py-12 lg:flex-row">
        <section className="flex flex-1 flex-col gap-8" aria-hidden="true">
          <span className="block h-8 w-56 animate-pulse rounded bg-surface-container-high" />
          <div className="space-y-4 rounded-xl border border-outline-variant p-5">
            <span className="block h-5 w-40 animate-pulse rounded bg-surface-container-high" />
            <div className="flex gap-4">
              <span className="h-24 w-24 shrink-0 animate-pulse rounded-lg bg-surface-container-high" />
              <div className="flex-1 space-y-2 pt-2">
                <span className="block h-4 w-2/3 animate-pulse rounded bg-surface-container-high" />
                <span className="block h-4 w-1/3 animate-pulse rounded bg-surface-container-high" />
              </div>
            </div>
          </div>
        </section>

        <aside className="w-full lg:w-[400px]" aria-hidden="true">
          <div className="space-y-4 rounded-xl border border-outline-variant p-5">
            <span className="block h-5 w-32 animate-pulse rounded bg-surface-container-high" />
            <span className="block h-4 w-full animate-pulse rounded bg-surface-container-high" />
            <span className="block h-4 w-full animate-pulse rounded bg-surface-container-high" />
            <span className="block h-14 w-full animate-pulse rounded-lg bg-surface-container-high" />
          </div>
        </aside>
      </main>
    </div>
  );
}
