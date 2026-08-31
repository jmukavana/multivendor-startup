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
