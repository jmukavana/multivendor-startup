import type { AuthField } from "./schemas";

/** Mirrors `iam.Role` on the backend. */
export type AccountRole =
  | "BUYER"
  | "VENDOR_OWNER"
  | "VENDOR_STAFF"
  | "PLATFORM_ADMIN"
  | "FINANCE_ADMIN"
  | "SUPER_ADMIN";

/**
 * What a caller elsewhere in the app may want to know after sign-in. Deliberately excludes the
 * session token — that is a secret meant to live only in the httpOnly cookie `actions.ts` sets,
 * never passed around as a typed value another feature might log or forward.
 */
export interface IssuedSession {
  readonly accountId: string;
  readonly roles: readonly AccountRole[];
  readonly canCheckOut: boolean;
}

export type AuthState =
  | { readonly status: "idle" }
  /** The client component reads this to advance from the identifier step to the code step. */
  | { readonly status: "code-sent"; readonly identifier: string }
  | {
      readonly status: "error";
      readonly message: string;
      /** Shown alongside the message so support can find the request in the logs. */
      readonly traceId?: string;
      readonly fieldErrors: Partial<Record<AuthField, string[]>>;
      readonly values: Partial<Record<AuthField, string>>;
    };

export const IDLE_AUTH_STATE: AuthState = { status: "idle" };
