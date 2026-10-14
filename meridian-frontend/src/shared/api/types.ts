/**
 * Wire types shared by every endpoint. These mirror `com.jamlapps.meridian.web` on the
 * backend — the two lists are a contract and change together.
 */

/** A backend `UUID`, which Jackson writes as its canonical string form. */
export type Uuid = string;

/** A backend `Instant`, which Jackson writes as an ISO-8601 string in UTC. */
export type IsoInstant = string;

/**
 * Mirrors `shared.Money`. The backend never sends a float: an amount is an integer count of
 * the currency's minor unit (cents for KES) plus the ISO-4217 code, so rounding is decided
 * once, on the server. Format it with `formatMoney` rather than dividing at the call site.
 */
export interface Money {
  readonly minorUnits: number;
  readonly currency: string;
}

/** Success envelope returned by every backend endpoint. */
export interface ApiResponse<T> {
  readonly data: T;
  readonly meta: ApiMeta;
}

export interface ApiMeta {
  readonly traceId?: string;
  readonly generatedAt: string;
}

export interface PaginatedMeta extends ApiMeta {
  readonly pagination: {
    readonly page: number;
    readonly perPage: number;
    readonly totalItems: number;
    readonly totalPages: number;
    readonly from: number;
    readonly to: number;
  };
  readonly sort: ReadonlyArray<{
    readonly field: string;
    readonly direction: string;
  }>;
  readonly appliedFilters: Record<string, string>;
}

export interface PaginatedResponse<T> {
  readonly data: readonly T[];
  readonly meta: PaginatedMeta;
}

/**
 * Page and per-page as every paginated endpoint takes them. `page` is zero-based on the way
 * in — `Pagination.of` converts it to a one-based number on the way out, so the `page` you
 * send and the `page` you read back off `meta` are deliberately different numbers.
 */
export interface PageRequest {
  readonly page?: number;
  readonly perPage?: number;
}

/** RFC 7807 problem details, as emitted by the backend's exception handler. */
export interface ApiProblem {
  readonly title: string;
  readonly status: number;
  readonly detail: string;
  readonly code: string;
  readonly traceId: string;
  readonly timestamp: string;
  /** Present on validation failures, keyed by field name. */
  readonly fieldErrors?: Record<string, string[]>;
}
