"use client";

import { useCart } from "@/features/cart";

/**
 * The live item count on the cart icon. Split out from `Shell` so the header and mobile nav
 * around it can stay server components — the cart itself only exists in the browser's
 * `localStorage`, so this one number is the only part of either that has to run on the client.
 */
export function CartBadge({
  className = "-right-0.5 -top-0.5 absolute",
}: {
  readonly className?: string;
}) {
  const { count, hydrated } = useCart();

  // Nothing before hydration: the server has no way to know what is in this browser's
  // localStorage, and guessing would either flash a wrong number or mismatch on hydration.
  if (!hydrated || count === 0) return null;

  return (
    <span
      className={`flex h-4 min-w-4 items-center justify-center rounded-full bg-error px-1 text-[10px] font-semibold text-on-error ${className}`}
    >
      {count > 99 ? "99+" : count}
    </span>
  );
}
