import { ChevronRight, Package } from "lucide-react";
import Link from "next/link";

import { listMyOrders } from "@/features/orders";
import {
  MarketplaceHeader,
  MobileNav,
} from "@/shared/components/marketplace/Shell";
import { Button } from "@/shared/components/ui/button";
import { Card, CardContent } from "@/shared/components/ui/card";
import { redirectIfSessionExpired, requireSession } from "@/shared/lib/guard";
import { formatMoney } from "@/shared/lib/money";

export const metadata = {
  title: "My Orders — Meridian",
};

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "Awaiting payment",
  CONFIRMED: "Confirmed",
  ACCEPTED: "Accepted",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

export default async function OrdersPage({
  searchParams,
}: PageProps<"/orders">) {
  await requireSession("/orders");

  const params = await searchParams;
  const page = Number(params.page) || 0;

  let result: Awaited<ReturnType<typeof listMyOrders>>;
  try {
    result = await listMyOrders({ page });
  } catch (error) {
    redirectIfSessionExpired(error, "/orders");
    throw error;
  }

  const { totalPages, page: currentPage } = result.meta.pagination;

  return (
    <div className="min-h-screen bg-surface pb-24 text-on-surface md:pb-0">
      <MarketplaceHeader active="orders" title="Meridian" />

      <main className="mx-auto w-full max-w-[1280px] px-margin-mobile py-8 md:px-margin-desktop md:py-12">
        <h1 className="mb-8 text-2xl font-bold leading-8 md:text-[32px] md:leading-10">
          My Orders
        </h1>

        {result.data.length === 0 ? (
          <div className="flex flex-col items-center gap-4 py-16 text-center">
            <Package className="h-10 w-10 text-on-surface-variant/40" />
            <p className="text-body-md text-on-surface-variant">
              You have not placed any orders yet.
            </p>
            <Button asChild>
              <Link href="/">Start shopping</Link>
            </Button>
          </div>
        ) : (
          <ul className="space-y-4">
            {result.data.map((order) => (
              <li key={order.id}>
                <Link href={`/orders/${order.id}`}>
                  <Card className="rounded-xl transition-shadow hover:shadow-[0px_8px_30px_rgba(0,0,0,0.12)]">
                    <CardContent className="flex items-center justify-between gap-4 p-5">
                      <div className="min-w-0">
                        <p className="font-mono text-code text-on-surface">
                          {order.reference}
                        </p>
                        <p className="mt-1 text-label-sm text-on-surface-variant">
                          {new Date(order.createdAt).toLocaleDateString(
                            "en-KE",
                            {
                              dateStyle: "medium",
                            },
                          )}
                          {" · "}
                          {order.subOrders.length} vendor
                          {order.subOrders.length === 1 ? "" : "s"}
                        </p>
                      </div>
                      <div className="flex shrink-0 items-center gap-4">
                        <span className="rounded-full bg-secondary-container px-3 py-1 text-label-md font-semibold text-on-secondary-container">
                          {STATUS_LABEL[order.status] ?? order.status}
                        </span>
                        <span className="text-title-md font-semibold">
                          {formatMoney(order.total)}
                        </span>
                        <ChevronRight className="h-5 w-5 text-on-surface-variant" />
                      </div>
                    </CardContent>
                  </Card>
                </Link>
              </li>
            ))}
          </ul>
        )}

        {totalPages > 1 && (
          <nav
            className="mt-10 flex items-center justify-center gap-3"
            aria-label="Pagination"
          >
            {currentPage > 1 && (
              <Button variant="outline" asChild>
                <Link href={`/orders?page=${page - 1}`}>Previous</Link>
              </Button>
            )}
            <span className="text-label-md text-on-surface-variant">
              Page {currentPage} of {totalPages}
            </span>
            {currentPage < totalPages && (
              <Button variant="outline" asChild>
                <Link href={`/orders?page=${page + 1}`}>Next</Link>
              </Button>
            )}
          </nav>
        )}
      </main>

      <MobileNav active="orders" />
    </div>
  );
}
