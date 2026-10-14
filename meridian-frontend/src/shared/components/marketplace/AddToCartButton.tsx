"use client";

import { Check, ShoppingCart } from "lucide-react";
import { useState } from "react";

import { useCart } from "@/features/cart";
import { Button } from "@/shared/components/ui/button";

interface AddToCartButtonProps {
  readonly productId: string;
  readonly productName: string;
  readonly quantity?: number;
  readonly size?: "icon" | "lg";
}

/** A brief confirmation, not a toast library — long enough to notice, short enough to not need dismissing. */
const CONFIRMATION_MS = 1500;

export function AddToCartButton({
  productId,
  productName,
  quantity = 1,
  size = "icon",
}: AddToCartButtonProps) {
  const { addItem } = useCart();
  const [added, setAdded] = useState(false);

  function handleClick() {
    addItem(productId, quantity);
    setAdded(true);
    window.setTimeout(() => setAdded(false), CONFIRMATION_MS);
  }

  if (size === "lg") {
    return (
      <Button
        variant="outline"
        size="lg"
        className="flex-1"
        onClick={handleClick}
        aria-label={`Add ${productName} to cart`}
      >
        {added ? (
          <Check className="h-5 w-5" />
        ) : (
          <ShoppingCart className="h-5 w-5" />
        )}
        {added ? "Added" : "Add to Cart"}
      </Button>
    );
  }

  return (
    <Button
      size="icon"
      variant="conversion"
      className="h-9 w-9 shrink-0 rounded-full"
      onClick={handleClick}
      aria-label={`Add ${productName} to cart`}
    >
      {added ? (
        <Check className="h-4 w-4" />
      ) : (
        <ShoppingCart className="h-4 w-4" />
      )}
    </Button>
  );
}
