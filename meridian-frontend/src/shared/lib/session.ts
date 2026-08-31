import "server-only";

import { cookies } from "next/headers";

/**
 * Session cookie issued by the backend. httpOnly, so the browser never holds a
 * token — every authenticated call is made server-side.
 */
export const SESSION_COOKIE = "meridian_session";

export async function getAccessToken(): Promise<string | null> {
  const store = await cookies();
  return store.get(SESSION_COOKIE)?.value ?? null;
}

export async function clearSession(): Promise<void> {
  const store = await cookies();
  store.delete(SESSION_COOKIE);
}
