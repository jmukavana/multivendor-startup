import "server-only";

import { cookies } from "next/headers";

/**
 * Session cookie holding the opaque token from POST /auth/verify (ADR-008).
 *
 * The backend returns the token in the response body, not a Set-Cookie header: the browser never
 * talks to it, only the Next.js server does, and Node's fetch would not forward that header
 * anyway. So the cookie is set here, from a Server Action, and read back on every server call.
 */
export const SESSION_COOKIE = "meridian_session";

export async function getAccessToken(): Promise<string | null> {
  const store = await cookies();
  return store.get(SESSION_COOKIE)?.value ?? null;
}

export async function createSession(
  token: string,
  expiresAt: string,
): Promise<void> {
  const store = await cookies();
  store.set(SESSION_COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === "production",
    // Lax rather than Strict: a buyer following an order link from an SMS or email arrives
    // cross-site, and Strict would drop the cookie and bounce them to sign in again.
    sameSite: "lax",
    path: "/",
    expires: new Date(expiresAt),
  });
}

export async function clearSession(): Promise<void> {
  const store = await cookies();
  store.delete(SESSION_COOKIE);
}
