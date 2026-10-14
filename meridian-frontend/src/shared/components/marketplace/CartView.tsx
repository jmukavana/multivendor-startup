"use client";

import {
  CreditCard,
  MapPin,
  Minus,
  Package,
  Plus,
  Store,
  Trash2,
} from "lucide-react";
import Image from "next/image";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";

import { signOut } from "@/features/auth";
import {
  type CartLineDetail,
  resolveCartLines,
  useCart,
} from "@/features/cart";
import { checkout, listCheckoutZones } from "@/features/checkout";
import type { DeliveryZone } from "@/features/fulfilment";
import type { Money } from "@/shared/api";
import { API_ERROR_CODES, isApiError } from "@/shared/api/error";
import { Button } from "@/shared/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/shared/components/ui/card";
import { Input } from "@/shared/components/ui/input";
import { Label } from "@/shared/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/components/ui/select";
import { formatMoney } from "@/shared/lib/money";

interface VendorGroup {
  readonly vendorId: string;
  readonly vendorName: string;
  readonly lines: readonly CartLineDetail[];
  readonly subtotal: Money;
}

function groupByVendor(details: readonly CartLineDetail[]): VendorGroup[] {
  const groups = new Map<string, VendorGroup>();

  for (const detail of details) {
    if (!detail.product) continue;
    const vendorId = detail.product.vendorId;
    const lineTotal = detail.product.price.minorUnits * detail.quantity;
    const existing = groups.get(vendorId);

    if (existing) {
      groups.set(vendorId, {
        ...existing,
        lines: [...existing.lines, detail],
        subtotal: {
          ...existing.subtotal,
          minorUnits: existing.subtotal.minorUnits + lineTotal,
        },
      });
    } else {
      groups.set(vendorId, {
        vendorId,
        vendorName: detail.vendorName ?? "Unknown vendor",
        lines: [detail],
        subtotal: {
          minorUnits: lineTotal,
          currency: detail.product.price.currency,
        },
      });
    }
  }

  return [...groups.values()];
}

export function CartView({ signedIn }: { readonly signedIn: boolean }) {
  const { lines, setQuantity, removeItem, clear } = useCart();
  const router = useRouter();

  const [details, setDetails] = useState<CartLineDetail[] | null>(null);
  const [zones, setZones] = useState<DeliveryZone[] | null>(null);
  const [zoneId, setZoneId] = useState("");
  const [phone, setPhone] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [idempotencyKey] = useState(() => crypto.randomUUID());

  useEffect(() => {
    let cancelled = false;
    if (lines.length === 0) {
      setDetails([]);
      return;
    }
    resolveCartLines(lines)
      .then((resolved) => {
        if (!cancelled) setDetails(resolved);
      })
      .catch(() => {
        // A resolution failure should not strand the page on "Loading your cart…" forever —
        // fall back to an empty resolved list so the rest of the page can still render.
        if (!cancelled) setDetails([]);
      });
    return () => {
      cancelled = true;
    };
  }, [lines]);

  useEffect(() => {
    if (!signedIn) return;
    listCheckoutZones()
      .then(setZones)
      .catch((error: unknown) => {
        // A cookie can outlive the session the backend still honours — e.g. after a fresh
        // deploy or reset. `signOut()` clears it before redirecting; the client cannot clear
        // an httpOnly cookie itself, and pushing to /login with it still set would just bounce
        // straight back here (login's own guard only checks that a cookie is present).
        if (
          isApiError(error) &&
          error.code === API_ERROR_CODES.SESSION_EXPIRED
        ) {
          signOut();
          return;
        }
        setFormError("We could not load delivery zones. Please try again.");
      });
  }, [signedIn]);

  const unavailable = useMemo(
    () => (details ?? []).filter((detail) => !detail.product),
    [details],
  );
  const groups = useMemo(() => groupByVendor(details ?? []), [details]);
  const itemCount = lines.reduce((sum, line) => sum + line.quantity, 0);
  const subtotal: Money = useMemo(() => {
    const currency = groups[0]?.subtotal.currency ?? "KES";
    return {
      currency,
      minorUnits: groups.reduce(
        (sum, group) => sum + group.subtotal.minorUnits,
        0,
      ),
    };
  }, [groups]);

  const canCheckout = groups.length > 0 && zoneId && phone.trim().length > 0;

  async function submitCheckout() {
    setSubmitting(true);
    setFormError(null);

    const result = await checkout(
      {
        lines: groups.flatMap((group) =>
          group.lines.map((line) => ({
            productId: line.productId,
            quantity: line.quantity,
          })),
        ),
        buyerPhoneNumber: phone,
        deliveryZoneId: zoneId,
      },
      idempotencyKey,
    );

    setSubmitting(false);

    if (result.status === "success") {
      clear();
      router.push(`/orders/${result.data.id}`);
      return;
    }

    setFormError(result.message);
  }

  if (details === null) {
    return (
      <p className="py-16 text-center text-body-md text-on-surface-variant">
        Loading your cart…
      </p>
    );
  }

  if (lines.length === 0) {
    return (
      <div className="flex flex-col items-center gap-4 py-16 text-center">
        <p className="text-body-md text-on-surface-variant">
          Your cart is empty.
        </p>
        <Button asChild>
          <Link href="/">Start shopping</Link>
        </Button>
      </div>
    );
  }

  return (
    <div className="flex w-full flex-col gap-gutter lg:flex-row">
      <section
        className="flex flex-1 flex-col gap-8"
        aria-labelledby="cart-title"
      >
        <h1
          id="cart-title"
          className="text-2xl font-bold leading-8 md:text-[32px] md:leading-10"
        >
          Your Cart ({itemCount} item{itemCount === 1 ? "" : "s"})
        </h1>

        {unavailable.length > 0 && (
          <Card className="border-error/40 bg-error-container/20 rounded-xl">
            <CardContent className="space-y-2 p-5">
              <p className="text-label-md font-semibold text-error">
                Some items in your cart are no longer available
              </p>
              {unavailable.map((line) => (
                <div
                  key={line.productId}
                  className="flex items-center justify-between gap-4 text-body-md"
                >
                  <span className="text-on-surface-variant">
                    Item removed by its vendor
                  </span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => removeItem(line.productId)}
                  >
                    Remove
                  </Button>
                </div>
              ))}
            </CardContent>
          </Card>
        )}

        {groups.map((group) => (
          <Card key={group.vendorId} className="rounded-xl">
            <CardHeader className="border-outline-variant border-b p-5">
              <div className="flex items-center gap-2">
                <Store className="h-5 w-5 text-secondary" />
                <CardTitle>
                  Sold by: <span className="font-bold">{group.vendorName}</span>
                </CardTitle>
              </div>
            </CardHeader>
            <CardContent className="space-y-6 p-5">
              {group.lines.map((line, index) => {
                const product = line.product;
                if (!product) return null;
                return (
                  <div
                    key={line.productId}
                    className={`flex flex-col gap-4 sm:flex-row sm:items-center ${
                      index > 0 ? "border-outline-variant/50 border-t pt-6" : ""
                    }`}
                  >
                    <div className="relative h-24 w-24 shrink-0 overflow-hidden rounded-lg bg-surface-variant">
                      {line.imageUrl ? (
                        <Image
                          src={line.imageUrl}
                          alt=""
                          fill
                          sizes="96px"
                          className="object-cover"
                        />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center text-on-surface-variant/40">
                          <Package className="h-8 w-8" />
                        </div>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <Link
                        href={`/product/${product.id}`}
                        className="mb-1 block text-title-md font-semibold hover:text-primary"
                      >
                        {product.name}
                      </Link>
                      <p className="text-label-md font-semibold text-primary">
                        {formatMoney(product.price)}
                      </p>
                    </div>
                    <div className="flex items-center gap-3">
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8 rounded-full"
                        aria-label="Decrease quantity"
                        onClick={() =>
                          setQuantity(line.productId, line.quantity - 1)
                        }
                      >
                        <Minus className="h-4 w-4" />
                      </Button>
                      <span className="w-4 text-center text-label-md">
                        {line.quantity}
                      </span>
                      <Button
                        variant="outline"
                        size="icon"
                        className="h-8 w-8 rounded-full"
                        aria-label="Increase quantity"
                        onClick={() =>
                          setQuantity(line.productId, line.quantity + 1)
                        }
                      >
                        <Plus className="h-4 w-4" />
                      </Button>
                    </div>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-error hover:bg-error-container"
                      aria-label={`Remove ${product.name}`}
                      onClick={() => removeItem(line.productId)}
                    >
                      <Trash2 className="h-5 w-5" />
                    </Button>
                  </div>
                );
              })}

              <div className="flex items-center justify-between gap-4 border-outline-variant border-t pt-4">
                <span className="text-body-md text-on-surface-variant">
                  Vendor Subtotal ({group.lines.length} item
                  {group.lines.length > 1 ? "s" : ""})
                </span>
                <span className="text-label-md font-semibold">
                  {formatMoney(group.subtotal)}
                </span>
              </div>
            </CardContent>
          </Card>
        ))}
      </section>

      <aside className="flex w-full flex-col gap-6 lg:w-[400px]">
        <Card className="sticky top-24 rounded-xl">
          <CardHeader>
            <CardTitle>Order Summary</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="mb-6 space-y-3 border-outline-variant border-b pb-6">
              <div className="flex items-center justify-between gap-4">
                <span className="text-body-md text-on-surface-variant">
                  Subtotal ({itemCount} item{itemCount === 1 ? "" : "s"})
                </span>
                <span className="text-label-md font-semibold">
                  {formatMoney(subtotal)}
                </span>
              </div>
              <p className="text-label-sm text-on-surface-variant">
                Delivery fees are calculated per vendor once you choose a
                delivery zone below.
              </p>
            </div>

            {!signedIn ? (
              <Button size="lg" className="h-14 w-full text-lg" asChild>
                <Link href="/login?next=/cart">Sign in to check out</Link>
              </Button>
            ) : (
              <div className="space-y-4">
                <div>
                  <Label htmlFor="cart-phone">M-Pesa phone number</Label>
                  <Input
                    id="cart-phone"
                    inputMode="tel"
                    placeholder="0712345678"
                    value={phone}
                    onChange={(event) => setPhone(event.target.value)}
                  />
                </div>

                <div>
                  <Label htmlFor="cart-zone">Delivery zone</Label>
                  {zones === null ? (
                    <p className="text-label-sm text-on-surface-variant">
                      Loading zones…
                    </p>
                  ) : (
                    <Select value={zoneId} onValueChange={setZoneId}>
                      <SelectTrigger id="cart-zone">
                        <SelectValue placeholder="Choose a delivery zone" />
                      </SelectTrigger>
                      <SelectContent>
                        {zones
                          .filter((zone) => zone.active)
                          .map((zone) => (
                            <SelectItem key={zone.id} value={zone.id}>
                              {zone.name} ({zone.county})
                            </SelectItem>
                          ))}
                      </SelectContent>
                    </Select>
                  )}
                </div>

                {formError && (
                  <p className="text-label-sm text-error" role="alert">
                    {formError}
                  </p>
                )}

                <Button
                  variant="conversion"
                  size="lg"
                  className="h-14 w-full bg-[#52B14C] text-lg hover:bg-[#43953e]"
                  disabled={!canCheckout || submitting}
                  onClick={submitCheckout}
                >
                  {submitting ? "Placing order…" : "Pay with M-PESA"}
                  <CreditCard className="h-5 w-5" />
                </Button>
                <p className="mt-4 flex items-center gap-1.5 text-center text-label-sm text-on-surface-variant">
                  <MapPin className="h-4 w-4 shrink-0" />
                  You will receive a prompt on your phone to enter your PIN.
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </aside>
    </div>
  );
}
