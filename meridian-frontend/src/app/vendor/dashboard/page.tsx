import { Box, PiggyBank, Wallet } from "lucide-react";
import Link from "next/link";

import { listMyProducts } from "@/features/catalog";
import { getStock } from "@/features/inventory";
import { listMyVendorSubOrders, subOrderTotal } from "@/features/orders";
import { getVendorBalance } from "@/features/settlement";
import { listMyVendorApplications } from "@/features/vendor-onboarding";
import { QuickAddProductForm } from "@/shared/components/marketplace/QuickAddProductForm";
import {
  MarketplaceHeader,
  MobileNav,
} from "@/shared/components/marketplace/Shell";
import { SubOrderActions } from "@/shared/components/marketplace/SubOrderActions";
import { VendorProductRow } from "@/shared/components/marketplace/VendorProductRow";
import { Button } from "@/shared/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
} from "@/shared/components/ui/card";
import { redirectIfSessionExpired, requireSession } from "@/shared/lib/guard";
import { formatMoney } from "@/shared/lib/money";

export const metadata = {
  title: "Vendor Dashboard — Meridian",
};

const STATUS_LABEL: Record<string, string> = {
  PENDING_PAYMENT: "Awaiting payment",
  CONFIRMED: "Confirmed",
  ACCEPTED: "Accepted",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

export default async function VendorDashboardPage() {
  await requireSession("/vendor/dashboard");

  let applications: Awaited<ReturnType<typeof listMyVendorApplications>>;
  try {
    applications = await listMyVendorApplications();
  } catch (error) {
    redirectIfSessionExpired(error, "/vendor/dashboard");
    throw error;
  }

  const approved = applications.find(
    (application) => application.status === "APPROVED" && application.vendorId,
  );

  if (!approved?.vendorId) {
    return (
      <div className="min-h-screen bg-background pb-24 text-on-background md:pb-0">
        <MarketplaceHeader active="home" />
        <main className="mx-auto flex w-full max-w-xl flex-col items-center gap-4 px-margin-mobile py-24 text-center">
          <h1 className="text-2xl font-bold">You are not a vendor yet</h1>
          <p className="text-body-md text-on-surface-variant">
            {applications.length > 0
              ? "Your vendor application has not been approved yet."
              : "Apply to sell on Meridian to get a dashboard."}
          </p>
          <Button asChild>
            <Link href="/vendor/apply">
              {applications.length > 0 ? "View your application" : "Apply now"}
            </Link>
          </Button>
        </main>
        <MobileNav active="home" />
      </div>
    );
  }

  const vendorId = approved.vendorId;

  let balance: Awaited<ReturnType<typeof getVendorBalance>>;
  let products: Awaited<ReturnType<typeof listMyProducts>>;
  let ordersPage: Awaited<ReturnType<typeof listMyVendorSubOrders>>;
  let stocks: Awaited<ReturnType<typeof getStock>>[];
  try {
    [balance, products, ordersPage] = await Promise.all([
      getVendorBalance(vendorId),
      listMyProducts(),
      listMyVendorSubOrders({ perPage: 10 }),
    ]);
    stocks = await Promise.all(products.map((product) => getStock(product.id)));
  } catch (error) {
    redirectIfSessionExpired(error, "/vendor/dashboard");
    throw error;
  }

  const activeProducts = products.filter(
    (product) => product.status === "ACTIVE",
  ).length;

  const metrics = [
    {
      label: "Payable Balance",
      value: formatMoney(balance.payable),
      icon: Wallet,
      tone: "bg-secondary-container text-on-secondary-container",
    },
    {
      label: "Held Back",
      value: formatMoney(balance.holdback),
      icon: PiggyBank,
      tone: "bg-primary-fixed text-on-primary-fixed",
    },
    {
      label: "Active Products",
      value: `${activeProducts} / ${products.length}`,
      icon: Box,
      tone: "bg-surface-variant text-on-surface-variant",
    },
  ] as const;

  return (
    <div className="min-h-screen bg-background pb-24 text-on-background md:pb-0">
      <MarketplaceHeader active="home" vendor />

      <main className="mx-auto flex w-full max-w-[1280px] flex-col gap-8 px-margin-mobile py-8 md:px-margin-desktop md:py-12">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="text-4xl font-bold leading-[44px] text-on-surface md:text-[48px] md:leading-[56px]">
              Overview
            </h1>
            <p className="mt-2 text-body-md text-on-surface-variant">
              {approved.businessName} — {ordersPage.meta.pagination.totalItems}{" "}
              order
              {ordersPage.meta.pagination.totalItems === 1 ? "" : "s"} total.
            </p>
          </div>
          <QuickAddProductForm />
        </div>

        <section
          className="grid grid-cols-1 gap-6 md:grid-cols-3"
          aria-label="Store summary"
        >
          {metrics.map((metric) => (
            <Card
              key={metric.label}
              className="flex h-40 flex-col justify-between rounded-xl transition-shadow hover:shadow-[0px_8px_30px_rgba(0,0,0,0.12)]"
            >
              <CardContent className="flex h-full flex-col justify-between p-6">
                <div className="flex items-center gap-3">
                  <div
                    className={`flex h-10 w-10 items-center justify-center rounded-full ${metric.tone}`}
                  >
                    <metric.icon className="h-5 w-5" />
                  </div>
                  <h2 className="text-title-md font-semibold">
                    {metric.label}
                  </h2>
                </div>
                <p className="text-2xl font-bold leading-8 md:text-[32px] md:leading-10">
                  {metric.value}
                </p>
              </CardContent>
            </Card>
          ))}
        </section>

        <Card className="overflow-hidden rounded-xl">
          <CardHeader className="border-outline-variant border-b">
            <CardTitle>Your Products</CardTitle>
          </CardHeader>
          {products.length === 0 ? (
            <p className="p-6 text-body-md text-on-surface-variant">
              You have not listed any products yet.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left">
                <thead>
                  <tr className="border-outline-variant border-b bg-surface-bright">
                    {["Product", "Price", "Status", "Stock", ""].map(
                      (heading) => (
                        <th
                          key={heading}
                          className="px-6 py-3 text-label-md font-semibold text-on-surface-variant"
                        >
                          {heading}
                        </th>
                      ),
                    )}
                  </tr>
                </thead>
                <tbody className="text-body-md">
                  {products.map((product, index) => (
                    <VendorProductRow
                      key={product.id}
                      product={product}
                      initialStock={stocks[index]}
                    />
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>

        <Card className="overflow-hidden rounded-xl">
          <CardHeader className="flex-row items-center justify-between border-outline-variant border-b">
            <CardTitle>Recent Orders</CardTitle>
          </CardHeader>
          {ordersPage.data.length === 0 ? (
            <p className="p-6 text-body-md text-on-surface-variant">
              No orders yet.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full border-collapse text-left">
                <thead>
                  <tr className="border-outline-variant border-b bg-surface-bright">
                    {["Sub-order", "Total", "Status", ""].map((heading) => (
                      <th
                        key={heading}
                        className="px-6 py-3 text-label-md font-semibold text-on-surface-variant"
                      >
                        {heading}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="text-body-md">
                  {ordersPage.data.map((subOrder) => (
                    <tr
                      key={subOrder.id}
                      className="border-outline-variant border-b transition-colors hover:bg-surface-container-low"
                    >
                      <td className="px-6 py-4 font-mono text-code text-on-surface">
                        {subOrder.id.slice(0, 8)}
                      </td>
                      <td className="px-6 py-4 font-semibold text-on-surface">
                        {formatMoney(subOrderTotal(subOrder))}
                      </td>
                      <td className="px-6 py-4">
                        <StatusBadge status={subOrder.status} />
                      </td>
                      <td className="px-6 py-4">
                        <SubOrderActions
                          subOrderId={subOrder.id}
                          status={subOrder.status}
                          cancellable={subOrder.cancellable}
                        />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </Card>
      </main>

      <MobileNav active="home" />
    </div>
  );
}

function StatusBadge({ status }: { readonly status: string }) {
  const className =
    status === "CONFIRMED"
      ? "bg-secondary-container text-on-secondary-container"
      : status === "ACCEPTED"
        ? "bg-primary-fixed text-on-primary-fixed"
        : status === "DELIVERED"
          ? "bg-surface-container-highest text-on-surface"
          : "bg-surface-variant text-on-surface-variant";

  return (
    <span className={`rounded-full px-2.5 py-0.5 text-label-sm ${className}`}>
      {STATUS_LABEL[status] ?? status}
    </span>
  );
}
