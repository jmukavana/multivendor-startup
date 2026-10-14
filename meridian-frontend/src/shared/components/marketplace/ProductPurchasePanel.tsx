"use client";

import { Bolt, Minus, Plus, ShoppingCart } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";

import { useCart } from "@/features/cart";
import { Button } from "@/shared/components/ui/button";

interface ProductPurchasePanelProps {
  readonly productId: string;
}

/** The sticky quantity + add-to-cart / buy-now bar at the foot of a product page. */
export function ProductPurchasePanel({ productId }: ProductPurchasePanelProps) {
  const { addItem } = useCart();
  const router = useRouter();
  const [quantity, setQuantity] = useState(1);

  function addToCart() {
    addItem(productId, quantity);
  }

  function buyNow() {
    addItem(productId, quantity);
    router.push("/cart");
  }

  return (
    <div className="fixed inset-x-0 bottom-0 z-40 border-t border-outline-variant bg-surface p-4 shadow-[0px_-4px_20px_rgba(0,0,0,0.08)] md:px-margin-desktop">
      <div className="mx-auto flex max-w-[1280px] items-center gap-4">
        <div className="flex items-center gap-3 rounded-lg border border-outline px-2 py-1">
          <Button
            variant="ghost"
            size="icon"
            aria-label="Decrease quantity"
            onClick={() => setQuantity((current) => Math.max(1, current - 1))}
          >
            <Minus className="h-4 w-4" />
          </Button>
          <span className="w-8 text-center text-body-md">{quantity}</span>
          <Button
            variant="ghost"
            size="icon"
            aria-label="Increase quantity"
            onClick={() => setQuantity((current) => current + 1)}
          >
            <Plus className="h-4 w-4" />
          </Button>
        </div>
        <Button
          variant="outline"
          size="lg"
          className="flex-1"
          onClick={addToCart}
        >
          <ShoppingCart className="h-5 w-5" />
          Add to Cart
        </Button>
        <Button
          variant="conversion"
          size="lg"
          className="flex-1"
          onClick={buyNow}
        >
          <Bolt className="h-5 w-5" />
          Buy Now
        </Button>
      </div>
    </div>
  );
}
