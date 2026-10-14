import { CartView } from "@/shared/components/marketplace/CartView";
import {
  MarketplaceHeader,
  MobileNav,
} from "@/shared/components/marketplace/Shell";
import { getAccessToken } from "@/shared/lib/session";

export const metadata = {
  title: "Cart and Checkout — Meridian",
};

export default async function CartPage() {
  const signedIn = Boolean(await getAccessToken());

  return (
    <div className="min-h-screen bg-background pb-24 text-on-background md:pb-0">
      <MarketplaceHeader active="cart" />

      <main className="mx-auto flex w-full max-w-[1280px] flex-col gap-gutter px-margin-mobile py-8 md:px-margin-desktop md:py-12">
        <CartView signedIn={signedIn} />
      </main>

      <MobileNav active="cart" />
    </div>
  );
}
