import "server-only";

import { getAccessToken } from "@/shared/lib/session";
import { toApiError } from "./error";
import type { ApiResponse } from "./types";

const BASE_URL = process.env.MERIDIAN_API_URL;
const DEFAULT_TIMEOUT_MS = 10_000;

type QueryValue = string | number | boolean | null | undefined;

export interface ApiRequestOptions {
  readonly method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  /** Serialised as JSON. Pass FormData for uploads — the multipart boundary is left alone. */
  readonly body?: unknown;
  readonly query?: Record<string, QueryValue>;
  readonly headers?: Record<string, string>;
  /**
   * Attach the session bearer token. Default true.
   * `false` also keeps the call out of request scope, so the route stays prerenderable.
   */
  readonly auth?: boolean;
  readonly tags?: string[];
  readonly revalidate?: number | false;
  /** Caching is opt-in in Next 16 — nothing is cached unless asked. */
  readonly cache?: RequestCache;
  /** Replayed on retry so the backend can discard duplicate writes. */
  readonly idempotencyKey?: string;
  readonly timeoutMs?: number;
  readonly signal?: AbortSignal;
}

function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  if (!BASE_URL) {
    throw new Error("MERIDIAN_API_URL is not set");
  }

  const url = new URL(
    path.replace(/^\//, ""),
    `${BASE_URL.replace(/\/$/, "")}/`,
  );
  for (const [key, value] of Object.entries(query ?? {})) {
    if (value !== undefined && value !== null) {
      url.searchParams.set(key, String(value));
    }
  }
  return url.toString();
}

async function parseBody(response: Response): Promise<unknown> {
  if (response.status === 204 || response.status === 205) return undefined;

  const text = await response.text();
  if (!text) return undefined;

  const contentType = response.headers.get("content-type") ?? "";
  if (!contentType.includes("json")) return text;

  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

async function request(
  path: string,
  options: ApiRequestOptions,
): Promise<unknown> {
  const {
    method = "GET",
    body,
    query,
    headers = {},
    auth = true,
    tags,
    revalidate,
    cache,
    idempotencyKey,
    timeoutMs = DEFAULT_TIMEOUT_MS,
    signal,
  } = options;

  const requestHeaders = new Headers({
    accept: "application/json",
    ...headers,
  });

  if (auth) {
    const token = await getAccessToken();
    if (!token) {
      throw toApiError(401, {
        code: "SESSION_EXPIRED",
        detail: "No active session",
      });
    }
    requestHeaders.set("authorization", `Bearer ${token}`);
  }

  if (idempotencyKey) {
    requestHeaders.set("idempotency-key", idempotencyKey);
  }

  const isFormData = body instanceof FormData;
  if (body !== undefined && !isFormData) {
    requestHeaders.set("content-type", "application/json");
  }

  const timeout = AbortSignal.timeout(timeoutMs);

  const response = await fetch(buildUrl(path, query), {
    method,
    headers: requestHeaders,
    body:
      body === undefined ? undefined : isFormData ? body : JSON.stringify(body),
    cache,
    signal: signal ? AbortSignal.any([timeout, signal]) : timeout,
    ...(tags || revalidate !== undefined ? { next: { tags, revalidate } } : {}),
  });

  const parsed = await parseBody(response);

  if (!response.ok) {
    throw toApiError(response.status, parsed);
  }

  return parsed;
}

/**
 * Server-side HTTP client for the Meridian backend.
 *
 * Throws {@link ApiRequestError} on any non-2xx and unwraps the `{ data, meta }`
 * envelope, so callers deal in domain objects and never inspect `response.ok`.
 */
export async function apiFetch<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const parsed = await request(path, options);
  return (parsed as ApiResponse<T> | undefined)?.data as T;
}

/** Keeps the envelope, for list endpoints whose `meta` carries pagination. */
export async function apiFetchEnvelope<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  return (await request(path, options)) as T;
}

type BodylessOptions = Omit<ApiRequestOptions, "method" | "body">;

export const api = {
  get: <T>(path: string, options?: BodylessOptions) =>
    apiFetch<T>(path, { ...options, method: "GET" }),

  post: <T>(path: string, body?: unknown, options?: BodylessOptions) =>
    apiFetch<T>(path, { ...options, method: "POST", body }),

  put: <T>(path: string, body?: unknown, options?: BodylessOptions) =>
    apiFetch<T>(path, { ...options, method: "PUT", body }),

  patch: <T>(path: string, body?: unknown, options?: BodylessOptions) =>
    apiFetch<T>(path, { ...options, method: "PATCH", body }),

  delete: <T>(path: string, options?: BodylessOptions) =>
    apiFetch<T>(path, { ...options, method: "DELETE" }),
};
