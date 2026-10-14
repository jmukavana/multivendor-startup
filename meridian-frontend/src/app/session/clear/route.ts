import { type NextRequest, NextResponse } from "next/server";

import { safeNextPath } from "@/features/auth";
import { clearSession } from "@/shared/lib/session";

/**
 * Clears a session cookie the backend no longer honours, then hands off to `/login`.
 *
 * Next only allows a cookie mutation inside a Server Action or a Route Handler (see
 * `cookies()`'s own restriction) — not inside a Server Component's render, which is exactly
 * where `redirectIfSessionExpired` discovers a session has expired. This route exists to give
 * that discovery a valid place to actually clear the cookie: `guard.ts` redirects here instead
 * of straight to `/login`, so the dead cookie is gone before `/login`'s own guard (which only
 * checks that a cookie is *present*, not that it still works) gets a chance to bounce the
 * visitor straight back to `next` and loop forever.
 */
export async function GET(request: NextRequest) {
  await clearSession();

  const next = safeNextPath(request.nextUrl.searchParams.get("next"));
  return NextResponse.redirect(
    new URL(`/login?next=${encodeURIComponent(next)}`, request.url),
  );
}
