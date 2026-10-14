import type { ApiProblem } from "./types";

export class ApiRequestError extends Error {
  readonly status: number;
  readonly code: string;
  readonly traceId: string;
  readonly timestamp: string;
  readonly fieldErrors: Record<string, string[]>;

  constructor(problem: ApiProblem) {
    super(problem.detail);
    this.name = "ApiRequestError";
    this.status = problem.status;
    this.code = problem.code;
    this.traceId = problem.traceId;
    this.timestamp = problem.timestamp;
    this.fieldErrors = problem.fieldErrors ?? {};
  }
}

export function isApiError(error: unknown): error is ApiRequestError {
  return error instanceof ApiRequestError;
}

/** Not every failure arrives as problem details — gateways and proxies emit HTML and bare strings. */
function extractDetail(body: unknown): string | null {
  if (typeof body === "string") {
    const trimmed = body.trim();
    return trimmed.length > 0 && !trimmed.startsWith("<") ? trimmed : null;
  }

  if (body && typeof body === "object") {
    for (const key of ["detail", "message", "error", "title"] as const) {
      const value = (body as Record<string, unknown>)[key];
      if (typeof value === "string" && value.trim().length > 0) {
        return value.trim();
      }
    }
  }

  return null;
}

export function toApiError(status: number, body: unknown): ApiRequestError {
  const problem = (body ?? {}) as Partial<ApiProblem>;

  return new ApiRequestError({
    title: problem.title ?? "Error",
    status: problem.status ?? status,
    detail: extractDetail(body) ?? `Request failed with status ${status}`,
    code: problem.code ?? "UNKNOWN_ERROR",
    traceId: problem.traceId ?? "",
    timestamp: problem.timestamp ?? new Date().toISOString(),
    fieldErrors: problem.fieldErrors,
  });
}

/**
 * Backend messages are written for operators, not buyers, and have no length
 * bound. Anything long or missing falls back to copy we control.
 */
const MAX_USER_FACING_LENGTH = 160;

export function getErrorMessage(
  error: unknown,
  fallback = "Something went wrong. Please try again.",
): string {
  if (!isApiError(error)) return fallback;

  const detail = error.message?.trim();
  return detail && detail.length <= MAX_USER_FACING_LENGTH ? detail : fallback;
}

/**
 * Mirrors `web.ErrorCode` in the backend. The two lists are a contract and change together;
 * a code missing here falls through to generic copy rather than failing.
 */
export const API_ERROR_CODES = {
  UNAUTHORIZED: "UNAUTHORIZED",
  FORBIDDEN: "FORBIDDEN",
  /** The only code that ends a session. A bare UNAUTHORIZED must not sign anyone out. */
  SESSION_EXPIRED: "SESSION_EXPIRED",
  NOT_FOUND: "NOT_FOUND",
  VALIDATION_ERROR: "VALIDATION_ERROR",
  KRA_PIN_TAKEN: "KRA_PIN_TAKEN",
  VENDOR_ALREADY_EXISTS: "VENDOR_ALREADY_EXISTS",
  RATE_LIMITED: "RATE_LIMITED",
  INTERNAL_ERROR: "INTERNAL_ERROR",
} as const;

export type ApiErrorCode =
  (typeof API_ERROR_CODES)[keyof typeof API_ERROR_CODES];
