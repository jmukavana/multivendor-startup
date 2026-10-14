"use server";

import { redirect } from "next/navigation";
import { z } from "zod";

import { api, getErrorMessage, isApiError } from "@/shared/api";
import {
  clearSession,
  createSession,
  getAccessToken,
} from "@/shared/lib/session";
import {
  type AuthField,
  normalizeIdentifier,
  readAuthForm,
  registerSchema,
  safeNextPath,
  signInSchema,
  verifyCodeSchema,
} from "./schemas";
import type { AuthState, IssuedSession } from "./types";

function failure(
  message: string,
  values: Partial<Record<AuthField, string>>,
  fieldErrors: Partial<Record<AuthField, string[]>> = {},
  traceId?: string,
): AuthState {
  return { status: "error", message, fieldErrors, values, traceId };
}

/**
 * Requests a code from whichever endpoint matches the caller's intent, then reports it as
 * "code-sent" either way — the shared shape both `registerAccount` and `requestSignInCode` reduce
 * to.
 *
 * Called with `auth: false` throughout this file: these are how a caller acquires a session in the
 * first place, so `apiFetch`'s default of requiring one already would make them uncallable while
 * signed out, which is every visitor who has not signed in yet.
 */
async function issueCode(
  endpoint: "/auth/register" | "/auth/code",
  identifier: string,
  displayName: string | undefined,
  values: Partial<Record<AuthField, string>>,
  failureMessage: string,
): Promise<AuthState> {
  try {
    await api.post(
      endpoint,
      displayName === undefined ? { identifier } : { identifier, displayName },
      { auth: false },
    );
  } catch (error) {
    if (!isApiError(error)) throw error;

    if (error.status === 400 || error.status === 422) {
      return failure(
        getErrorMessage(error, "Check the highlighted fields and try again."),
        values,
        error.fieldErrors as Partial<Record<AuthField, string[]>>,
        error.traceId,
      );
    }

    return failure(
      getErrorMessage(error, failureMessage),
      values,
      {},
      error.traceId,
    );
  }

  return { status: "code-sent", identifier };
}

/** BR-IAM-01: creates the account (idempotently) and issues its first code. */
export async function registerAccount(
  _previous: AuthState,
  formData: FormData,
): Promise<AuthState> {
  const values = readAuthForm(formData);
  const parsed = registerSchema.safeParse(values);

  if (!parsed.success) {
    const { fieldErrors } = z.flattenError(parsed.error);
    return failure(
      "Check the highlighted fields and try again.",
      values,
      fieldErrors,
    );
  }

  const identifier = normalizeIdentifier(parsed.data.identifier);
  if (!identifier) {
    return failure(
      "Enter a Kenyan mobile number or a valid email address.",
      values,
      { identifier: ["Enter a Kenyan mobile number or a valid email address"] },
    );
  }

  return issueCode(
    "/auth/register",
    identifier,
    parsed.data.displayName,
    values,
    "We could not create your account. Please try again shortly.",
  );
}

/**
 * BR-IAM-02: requests a code for a returning identifier via the silent endpoint — it gives no
 * signal either way about whether the identifier exists, matching `AuthenticationService`'s design
 * (see its class doc) — rather than `/auth/register`, which would also work but requires a display
 * name this flow has no reason to ask for again.
 */
export async function requestSignInCode(
  _previous: AuthState,
  formData: FormData,
): Promise<AuthState> {
  const values = readAuthForm(formData);
  const parsed = signInSchema.safeParse(values);

  if (!parsed.success) {
    const { fieldErrors } = z.flattenError(parsed.error);
    return failure(
      "Check the highlighted fields and try again.",
      values,
      fieldErrors,
    );
  }

  const identifier = normalizeIdentifier(parsed.data.identifier);
  if (!identifier) {
    return failure(
      "Enter a Kenyan mobile number or a valid email address.",
      values,
      { identifier: ["Enter a Kenyan mobile number or a valid email address"] },
    );
  }

  return issueCode(
    "/auth/code",
    identifier,
    undefined,
    values,
    "We could not send a verification code. Please try again shortly.",
  );
}

interface VerifyResponse extends IssuedSession {
  readonly token: string;
  readonly expiresAt: string;
}

/** BR-IAM-02/05: verifies the code and, on success, creates the session cookie and redirects. */
export async function verifySignInCode(
  _previous: AuthState,
  formData: FormData,
): Promise<AuthState> {
  const values = readAuthForm(formData);
  const parsed = verifyCodeSchema.safeParse(values);

  if (!parsed.success) {
    const { fieldErrors } = z.flattenError(parsed.error);
    return failure("Check the code and try again.", values, fieldErrors);
  }

  let session: VerifyResponse;
  try {
    session = await api.post<VerifyResponse>(
      "/auth/verify",
      { identifier: parsed.data.identifier, code: parsed.data.code },
      { auth: false },
    );
  } catch (error) {
    if (!isApiError(error)) throw error;

    if (error.status === 400) {
      return failure(
        "Check the code and try again.",
        values,
        error.fieldErrors as Partial<Record<AuthField, string[]>>,
      );
    }

    // UNAUTHORIZED (wrong or expired code) and FORBIDDEN (account not available) both get the
    // same generic copy on purpose — AuthenticationService deliberately does not distinguish them
    // in its own response, so there is nothing more specific to say here either.
    return failure(
      getErrorMessage(error, "That code is not valid or has expired."),
      values,
      {},
      error.traceId,
    );
  }

  await createSession(session.token, session.expiresAt);

  // Re-validated here, not only where `next` first entered the page: a Server Action is reachable
  // by a direct POST that skips whatever sanitised it into the hidden field in the first place.
  const next = safeNextPath(formData.get("next")?.toString());
  redirect(next);
}

/** Signs out the current session only. */
export async function signOut(): Promise<void> {
  if (await getAccessToken()) {
    try {
      await api.delete("/auth/session");
    } catch (error) {
      if (!isApiError(error)) throw error;
      // The visitor's intent is to no longer be signed in locally, which clearSession() below
      // guarantees regardless of whether the backend call itself succeeded — a network blip here
      // must not leave someone unable to sign out.
    }
  }

  await clearSession();
  redirect("/");
}

/** BR-IAM: signs out every session for the account, not only the caller's. */
export async function signOutEverywhere(): Promise<void> {
  try {
    await api.delete("/auth/sessions");
  } catch (error) {
    if (!isApiError(error)) throw error;
  }

  await clearSession();
  redirect("/");
}
