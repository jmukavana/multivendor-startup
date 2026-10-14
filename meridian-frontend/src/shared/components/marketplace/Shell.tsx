import type { LucideIcon } from "lucide-react";
import {
  ArrowLeft,
  Home,
  ListOrdered,
  Menu,
  Search,
  ShoppingCart,
  Store,
} from "lucide-react";
import Link from "next/link";

import { signOut } from "@/features/auth";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { getAccessToken } from "@/shared/lib/session";
import { CartBadge } from "./CartBadge";

const buyerNav = [
  { href: "/", label: "Home" },
  { href: "/orders", label: "Orders" },
  { href: "/vendor/apply", label: "Sell on Meridian" },
] as const;

const vendorNav = [
  { href: "/vendor/dashboard", label: "Dashboard" },
  { href: "/", label: "Storefront" },
] as const;

interface MarketplaceHeaderProps {
  readonly title?: string;
  readonly active?: "home" | "cart" | "orders";
  readonly backHref?: string;
  readonly vendor?: boolean;
}

export async function MarketplaceHeader({
  title,
  active = "home",
  backHref,
  vendor = false,
}: MarketplaceHeaderProps) {
  const signedIn = Boolean(await getAccessToken());
  const links = vendor ? vendorNav : buyerNav;

  return (
    <header className="sticky top-0 z-40 border-b border-outline-variant bg-surface/95 shadow-sm backdrop-blur">
      <div className="mx-auto flex h-16 max-w-[1280px] items-center gap-4 px-margin-mobile md:px-margin-desktop">
        {backHref ? (
          <Button variant="ghost" size="icon" asChild className="rounded-full">
            <Link href={backHref} aria-label="Go back">
              <ArrowLeft className="h-5 w-5" />
            </Link>
          </Button>
        ) : (
          <Button
            variant="ghost"
            size="icon"
            className="hidden rounded-full md:inline-flex"
            aria-label="Open menu"
          >
            <Menu className="h-5 w-5" />
          </Button>
        )}

        <Link
          href={vendor ? "/vendor/dashboard" : "/"}
          className="shrink-0 text-2xl font-bold text-primary md:text-[32px] md:leading-10"
        >
          {title ?? "Meridian"}
        </Link>

        {!vendor && !backHref && (
          <form action="/" method="get" className="min-w-0 flex-1 md:px-4">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-on-surface-variant" />
              <Input
                type="search"
                name="q"
                aria-label="Search products"
                placeholder="Search products"
                className="h-12 rounded-full bg-surface-container-low pl-11 text-base md:h-11"
              />
            </div>
          </form>
        )}

        <nav
          className="ml-auto hidden items-center gap-1 md:flex"
          aria-label="Primary"
        >
          {links.map((link) => {
            const isActive = vendor
              ? link.href === "/vendor/dashboard" && active === "home"
              : (link.href === "/" && active === "home") ||
                (link.href === "/orders" && active === "orders");

            return (
              <Button
                key={link.href}
                variant="ghost"
                asChild
                className={
                  isActive ? "text-primary" : "text-on-surface-variant"
                }
              >
                <Link href={link.href}>{link.label}</Link>
              </Button>
            );
          })}

          {signedIn ? (
            <form action={signOut}>
              <Button
                variant="ghost"
                type="submit"
                className="text-on-surface-variant"
              >
                Sign out
              </Button>
            </form>
          ) : (
            <Button variant="ghost" asChild className="text-on-surface-variant">
              <Link href="/login">Sign in</Link>
            </Button>
          )}

          {!vendor && (
            <Button
              variant="ghost"
              size="icon"
              asChild
              className="relative rounded-full"
            >
              <Link href="/cart" aria-label="Cart">
                <ShoppingCart className="h-5 w-5" />
                <CartBadge />
              </Link>
            </Button>
          )}
        </nav>
      </div>
    </header>
  );
}

const mobileNav = [
  { href: "/", label: "Home", icon: Home },
  { href: "/orders", label: "Orders", icon: ListOrdered },
  { href: "/cart", label: "Cart", icon: ShoppingCart, cart: true },
  { href: "/vendor/apply", label: "Sell", icon: Store },
] as const;

export async function MobileNav({
  active = "home",
}: {
  readonly active?: "home" | "cart" | "orders";
}) {
  return (
    <nav
      className="fixed inset-x-0 bottom-0 z-50 rounded-t-xl border-t border-outline-variant bg-surface-container-lowest px-4 py-3 shadow-[0px_-4px_20px_rgba(0,0,0,0.08)] md:hidden"
      aria-label="Mobile navigation"
    >
      <div className="mx-auto grid max-w-md grid-cols-4 gap-1">
        {mobileNav.map((item) => (
          <MobileNavItem
            key={item.href}
            href={item.href}
            label={item.label}
            active={active === item.label.toLowerCase()}
            icon={item.icon}
            cart={"cart" in item && item.cart}
          />
        ))}
      </div>
    </nav>
  );
}

function MobileNavItem({
  href,
  label,
  icon: Icon,
  active,
  cart,
}: {
  readonly href: string;
  readonly label: string;
  readonly icon: LucideIcon;
  readonly active: boolean;
  readonly cart?: boolean;
}) {
  return (
    <Link
      href={href}
      className="flex min-h-14 flex-col items-center justify-center gap-1 text-xs text-on-surface-variant"
      aria-current={active ? "page" : undefined}
    >
      <span
        className={`relative flex h-9 w-14 items-center justify-center rounded-full ${
          active ? "bg-secondary-fixed text-on-secondary-fixed-variant" : ""
        }`}
      >
        <Icon className="h-5 w-5" />
        {cart && <CartBadge className="-right-0.5 absolute top-1" />}
      </span>
      <span className={active ? "text-on-secondary-fixed-variant" : ""}>
        {label}
      </span>
    </Link>
  );
}
