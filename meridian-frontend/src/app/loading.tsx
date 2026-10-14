/**
 * The home route's loading state — shown while `HomePage` resolves its data fetches. Mirrors
 * that page's real layout (same max-width, same grid) so nothing shifts once the real content
 * swaps in, rather than a generic spinner unrelated to what is about to appear.
 */
export default function HomeLoading() {
  return (
    <div className="min-h-screen bg-surface pb-24 text-on-surface md:pb-0">
      <div className="sticky top-0 z-40 border-b border-outline-variant bg-surface/95 backdrop-blur">
        <div className="mx-auto flex h-16 max-w-[1280px] items-center gap-4 px-margin-mobile md:px-margin-desktop">
          <span className="h-7 w-28 shrink-0 animate-pulse rounded bg-surface-container-high" />
          <span className="h-11 min-w-0 flex-1 animate-pulse rounded-full bg-surface-container-low md:h-11" />
        </div>
      </div>

      <main className="mx-auto max-w-[1280px] px-margin-mobile py-8 md:px-margin-desktop">
        <div className="mb-12 flex gap-3 md:mb-14" aria-hidden="true">
          {["w-24", "w-20", "w-36", "w-40"].map((width) => (
            <span
              key={width}
              className={`h-11 shrink-0 animate-pulse rounded-full bg-surface-container-high ${width}`}
            />
          ))}
        </div>

        <span className="mb-6 block h-8 w-64 animate-pulse rounded bg-surface-container-high" />

        <div className="grid grid-cols-2 gap-5 md:grid-cols-4 md:gap-gutter lg:grid-cols-5">
          {Array.from({ length: 10 }, (_, index) => (
            <div
              // biome-ignore lint/suspicious/noArrayIndexKey: a fixed count of interchangeable, unordered skeleton placeholders
              key={index}
              className="overflow-hidden rounded-xl border border-outline-variant/50"
            >
              <div className="aspect-square animate-pulse bg-surface-container-high" />
              <div className="space-y-2 p-4">
                <span className="block h-4 w-3/4 animate-pulse rounded bg-surface-container-high" />
                <span className="block h-4 w-1/2 animate-pulse rounded bg-surface-container-high" />
                <span className="block h-5 w-1/3 animate-pulse rounded bg-surface-container-high" />
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
