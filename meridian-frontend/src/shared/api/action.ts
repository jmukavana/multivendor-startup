import { z } from "zod";

import type { ActionFailure, ActionResult } from "./action-state";
import { API_ERROR_CODES, getErrorMessage, isApiError } from "./error";

/**
 * Helpers every Server Action shares.
 *
 * Actions do not throw across the network boundary — an `ApiRequestError` raised on the server
 * reaches the browser as an opaque "an error occurred" digest — so every mutation reduces both
 * outcomes to an `ActionResult` instead.
 */

export function succeeded<T>(data: T): ActionResult<T> {
  return { status: "success", data };
}

export function failed(
  message: string,
  code: string = API_ERROR_CODES.VALIDATION_ERROR,
  fieldErrors: Record<string, string[]> = {},
  traceId?: string,
): ActionFailure {
  return { status: "error", code, message, fieldErrors, traceId };
}

/**
 * Converts a thrown `ApiRequestError` into a failure result.
 *
 * Anything else is rethrown untouched — that includes `redirect()` and `notFound()`, which
 * signal themselves by throwing and would be silently swallowed if this caught everything.
 */
export function toActionFailure(
  error: unknown,
  fallback: string,
): ActionFailure {
  if (!isApiError(error)) throw error;

  return {
    status: "error",
    code: error.code,
    message: getErrorMessage(error, fallback),
    traceId: error.traceId || undefined,
    fieldErrors: error.fieldErrors,
  };
}

/** Runs a backend call and reduces both outcomes to an `ActionResult`. */
export async function attempt<T>(
  fallback: string,
  run: () => Promise<T>,
): Promise<ActionResult<T>> {
  try {
    return succeeded(await run());
  } catch (error) {
    return toActionFailure(error, fallback);
  }
}

/**
 * Validates a Server Action's arguments.
 *
 * A Server Action is a public POST endpoint, reachable without ever loading the form that is
 * supposed to feed it, so its arguments are untrusted input however well-typed they look on
 * the client. Every action in this codebase parses before it calls the backend.
 */
export function parseInput<S extends z.ZodType>(
  schema: S,
  input: unknown,
): { ok: true; data: z.infer<S> } | { ok: false; failure: ActionFailure } {
  const parsed = schema.safeParse(input);
  if (parsed.success) return { ok: true, data: parsed.data };

  const { fieldErrors } = z.flattenError(parsed.error);
  return {
    ok: false,
    failure: failed(
      "Check the highlighted fields and try again.",
      API_ERROR_CODES.VALIDATION_ERROR,
      fieldErrors as Record<string, string[]>,
    ),
  };
}
