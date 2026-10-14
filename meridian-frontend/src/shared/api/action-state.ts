/**
 * The shape a Server Action resolves to, and its idle starting value.
 *
 * Deliberately in its own module with no `server-only` import: a client component driving a
 * form with `useActionState` needs these, and everything else reachable from the
 * `@/shared/api` barrel — the HTTP client above all — is server-only. Import from
 * `@/shared/api/action-state` on the client, from `@/shared/api` on the server.
 */

export interface ActionFailure {
  readonly status: "error";
  /** The backend's `ErrorCode`, or "UNKNOWN_ERROR". Branch on this, never on the message. */
  readonly code: string;
  readonly message: string;
  /** Shown alongside the message so support can find the request in the logs. */
  readonly traceId?: string;
  readonly fieldErrors: Record<string, string[]>;
}

export type ActionResult<T> =
  | { readonly status: "success"; readonly data: T }
  | ActionFailure;

/** `useActionState` needs a starting value that is neither a success nor a failure. */
export type ActionState<T> = { readonly status: "idle" } | ActionResult<T>;

export const IDLE_ACTION_STATE = { status: "idle" } as const;
