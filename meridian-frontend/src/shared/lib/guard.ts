import "server-only";

import { redirect } from "next/navigation";

import { API_ERROR_CODES, isApiError } from "@/shared/api";
import { getAccessToken } from "./session";

/**
 * Sends a signed-out caller to sign in, remembering where they were headed.
 *
 * Called at the top of every authenticated page and Server Action. An action is reachable by
 * a direct POST that never rendered the page, so the page's own guard is not enough.
 */
export async function requireSession(next: string): Promise<string> {
  const token = await getAccessToken();
  if (!token) redirect(`/login?next=${encodeURIComponent(next)}`);
  return token;
}

/**
 * True only for a session the backend has explicitly retired.
 *
 * A bare 401 or a 403 is not the same thing — it can mean a permission this caller lacks or a
 * backend hiccup, and treating either as an expiry signs people out while their cookie is
 * still perfectly valid.
 */
export function isSessionExpired(error: unknown): boolean {
  return isApiError(error) && error.code === API_ERROR_CODES.SESSION_EXPIRED;
}

/**
 * Re-authenticates when, and only when, the backend says the session is gone.
 *
 * Goes through `/session/clear`, not straight to `/login` — a cookie can only be mutated inside
 * a Server Action or a Route Handler, never inside a Server Component's render, which is where
 * this is normally called from. `/login`'s own guard only checks that a cookie is *present*, not
 * that the backend still honours it, so redirecting there with the dead cookie still set would
 * send it straight back to `next` and loop forever — `/session/clear` clears it first.
 */
export function redirectIfSessionExpired(error: unknown, next: string): void {
  if (isSessionExpired(error)) {
    redirect(`/session/clear?next=${encodeURIComponent(next)}`);
  }
}
