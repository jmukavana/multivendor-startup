import type { NextRequest } from "next/server";

/**
 * Streams a public backend resource (currently: product images) to the browser.
 *
 * The browser cannot fetch `MERIDIAN_API_URL` directly — in production that address is only
 * reachable from this Next server, the same reason every other read in this app goes through a
 * Server Component or Server Action rather than a client-side `fetch`. An `<img src>` has to be
 * something the browser itself can hit, so this route re-fetches the backend resource here and
 * relays the bytes, rather than ever exposing the backend's own address to the client.
 */
const BASE_URL = process.env.MERIDIAN_API_URL;

export async function GET(
  _request: NextRequest,
  context: RouteContext<"/img/[...segments]">,
) {
  if (!BASE_URL) {
    return new Response("MERIDIAN_API_URL is not set", { status: 500 });
  }

  const { segments } = await context.params;
  const upstream = await fetch(
    `${BASE_URL.replace(/\/$/, "")}/${segments.map(encodeURIComponent).join("/")}`,
    { headers: { accept: "image/*" } },
  );

  if (!upstream.ok || !upstream.body) {
    return new Response(null, { status: upstream.status === 404 ? 404 : 502 });
  }

  return new Response(upstream.body, {
    status: 200,
    headers: {
      "content-type":
        upstream.headers.get("content-type") ?? "application/octet-stream",
      "cache-control":
        upstream.headers.get("cache-control") ?? "public, max-age=86400",
    },
  });
}
