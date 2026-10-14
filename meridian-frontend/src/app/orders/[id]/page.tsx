import { CheckCircle2, MapPin, Package, Store, Truck } from "lucide-react";
import { notFound } from "next/navigation";

import { type Delivery, getDeliveryForSubOrder } from "@/features/fulfilment";
import {
  getOrder,
  type ParentOrder,
  type SubOrder,
  subOrderTotal,
} from "@/features/orders";
import { resolveVendorNames } from "@/features/vendors";
import { API_ERROR_CODES, isApiError } from "@/shared/api";
import {
  MarketplaceHeader,
  MobileNav,
} from "@/shared/components/marketplace/Shell";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/shared/components/ui/card";
import { redirectIfSessionExpired, requireSession } from "@/shared/lib/guard";
import { formatMoney } from "@/shared/lib/money";

export const metadata = {
  title: "Order details — Meridian",
};

const ORDER_STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "Awaiting payment",
  CONFIRMED: "Confirmed",
  ACCEPTED: "Accepted",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

const DELIVERY_STATUS_LABEL: Record<string, string> = {
  PENDING: "Preparing for dispatch",
  BOOKED: "Booked with carrier",
  DISPATCHED: "Dispatched",
  IN_TRANSIT: "On the way",
  DELIVERED: "Delivered",
  FAILED: "Delivery failed",
};

async function loadOrder(orderId: string, next: string): Promise<ParentOrder> {
  try {
    return await getOrder(orderId);
  } catch (error) {
    if (isApiError(error) && error.code === API_ERROR_CODES.NOT_FOUND)
      notFound();
    redirectIfSessionExpired(error, next);
    throw error;
  }
}

export default async function OrderDetailPage({
  params,
}: PageProps<"/orders/[id]">) {
  const { id } = await params;
  const next = `/orders/${id}`;
  await requireSession(next);

  const order = await loadOrder(id, next);

  const [vendorNames, deliveries] = await Promise.all([
    resolveVendorNames(order.subOrders.map((subOrder) => subOrder.vendorId)),
    Promise.all(
      order.subOrders.map((subOrder) =>
        getDeliveryForSubOrder(subOrder.id).catch(() => null),
      ),
    ),
  ]);

  return (
    <div className="min-h-screen bg-surface pb-24 text-on-surface md:pb-0">
      <MarketplaceHeader backHref="/orders" title="Meridian" />

      <main className="mx-auto w-full max-w-[1280px] px-margin-mobile py-8 md:px-margin-desktop md:py-12">
        <div className="mb-8 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="mb-1 text-2xl font-bold leading-8 md:text-[32px] md:leading-10">
              Order {order.reference}
            </h1>
            <p className="text-body-md text-on-surface-variant">
              Placed on{" "}
              {new Date(order.createdAt).toLocaleDateString("en-KE", {
                dateStyle: "long",
              })}
            </p>
          </div>
          <div className="flex items-center gap-3">
            <span className="inline-flex items-center gap-1 rounded-full bg-secondary-container px-3 py-1 text-label-md font-semibold text-on-secondary-container">
              <CheckCircle2 className="h-4 w-4" />
              {ORDER_STATUS_LABEL[order.status] ?? order.status}
            </span>
            <p className="text-title-md font-semibold">
              {formatMoney(order.total)}
            </p>
          </div>
        </div>

        <div className="space-y-6">
          {order.subOrders.map((subOrder, index) => (
            <SubOrderCard
              key={subOrder.id}
              subOrder={subOrder}
              vendorName={
                vendorNames.get(subOrder.vendorId) ?? "Unknown vendor"
              }
              delivery={deliveries[index]}
            />
          ))}
        </div>
      </main>

      <MobileNav active="orders" />
    </div>
  );
}

function SubOrderCard({
  subOrder,
  vendorName,
  delivery,
}: {
  readonly subOrder: SubOrder;
  readonly vendorName: string;
  readonly delivery: Delivery | null;
}) {
  return (
    <Card className="overflow-hidden">
      <CardHeader className="flex-row items-center justify-between border-outline-variant border-b bg-surface/50 space-y-0">
        <CardTitle className="flex items-center gap-2">
          <Store className="h-4 w-4 text-primary" />
          {vendorName}
        </CardTitle>
        <span className="rounded-full bg-secondary-container px-2.5 py-0.5 text-label-sm text-on-secondary-container">
          {ORDER_STATUS_LABEL[subOrder.status] ?? subOrder.status}
        </span>
      </CardHeader>
      <CardContent className="space-y-6 p-6">
        {delivery && (
          <div className="flex items-start gap-3 rounded-lg bg-surface-container-low p-4">
            <Truck className="h-5 w-5 shrink-0 text-secondary" />
            <div className="text-body-md">
              <p className="font-semibold">
                {DELIVERY_STATUS_LABEL[delivery.status] ?? delivery.status}
              </p>
              {delivery.trackingReference && (
                <p className="mt-1 text-label-sm text-on-surface-variant">
                  Tracking reference: {delivery.trackingReference}
                </p>
              )}
              {delivery.dispatchedAt && (
                <p className="mt-1 flex items-center gap-1 text-label-sm text-on-surface-variant">
                  <MapPin className="h-3.5 w-3.5" />
                  Dispatched{" "}
                  {new Date(delivery.dispatchedAt).toLocaleString("en-KE", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}
                </p>
              )}
              {delivery.deliveredAt && (
                <p className="mt-1 text-label-sm text-on-surface-variant">
                  Delivered{" "}
                  {new Date(delivery.deliveredAt).toLocaleString("en-KE", {
                    dateStyle: "medium",
                    timeStyle: "short",
                  })}
                </p>
              )}
            </div>
          </div>
        )}

        <div className="space-y-4">
          {subOrder.lines.map((line) => (
            <div key={line.productId} className="flex items-start gap-4">
              <div className="flex h-16 w-16 shrink-0 items-center justify-center rounded-lg border border-outline-variant/20 bg-surface-container-low text-on-surface-variant/40">
                <Package className="h-6 w-6" />
              </div>
              <div className="min-w-0 flex-1">
                <h3 className="text-label-md font-semibold">
                  {line.productNameSnapshot}
                </h3>
                <p className="mt-1 text-label-sm text-on-surface-variant">
                  Qty: {line.quantity}
                </p>
              </div>
              <span className="shrink-0 text-right text-label-md font-semibold">
                {formatMoney(line.lineTotal)}
              </span>
            </div>
          ))}
        </div>

        <div className="flex items-center justify-between gap-4 border-outline-variant border-t pt-4 text-body-md">
          <span className="text-on-surface-variant">Delivery</span>
          <span className="font-semibold">
            {formatMoney(subOrder.deliveryCost)}
          </span>
        </div>
        <div className="flex items-center justify-between gap-4 text-title-md font-semibold">
          <span>Total</span>
          <span>{formatMoney(subOrderTotal(subOrder))}</span>
        </div>
      </CardContent>
    </Card>
  );
}
