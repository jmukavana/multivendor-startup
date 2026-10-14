"use client";

import { useState, useTransition } from "react";

import { delistProduct, publishProduct } from "@/features/catalog/actions";
import type { Product } from "@/features/catalog/types";
import { setStock } from "@/features/inventory/actions";
import type { StockView } from "@/features/inventory/types";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { formatMoney } from "@/shared/lib/money";

/**
 * `getStock` is `"server-only"` (see its own doc) — it cannot be imported into this client
 * component. `initialStock` is fetched by the server component that renders this row instead,
 * the same split `resolveCartLines` uses for cart lines it cannot fetch client-side either.
 */
interface VendorProductRowProps {
  readonly product: Product;
  readonly initialStock: StockView;
}

export function VendorProductRow({
  product,
  initialStock,
}: VendorProductRowProps) {
  const [stock, setStockView] = useState(initialStock);
  const [quantityInput, setQuantityInput] = useState(
    String(initialStock.totalQuantity),
  );
  const [pending, startTransition] = useTransition();
  const [error, setError] = useState<string | null>(null);

  function updateStock() {
    const totalQuantity = Number.parseInt(quantityInput, 10);
    if (Number.isNaN(totalQuantity) || totalQuantity < 0) {
      setError("Enter a whole, non-negative number.");
      return;
    }

    setError(null);
    startTransition(async () => {
      const result = await setStock({ productId: product.id, totalQuantity });
      if (result.status === "success") {
        setStockView(result.data);
      } else {
        setError(result.message);
      }
    });
  }

  function togglePublish() {
    setError(null);
    startTransition(async () => {
      const action =
        product.status === "ACTIVE" ? delistProduct : publishProduct;
      const result = await action({ productId: product.id });
      if (result.status === "error") setError(result.message);
    });
  }

  return (
    <tr className="border-outline-variant border-b">
      <td className="px-6 py-4 text-on-surface">{product.name}</td>
      <td className="px-6 py-4 text-on-surface-variant">
        {formatMoney(product.price)}
      </td>
      <td className="px-6 py-4">
        <span className="rounded-full bg-surface-variant px-2.5 py-0.5 text-label-sm text-on-surface-variant">
          {product.status}
        </span>
      </td>
      <td className="px-6 py-4">
        <div className="flex items-center gap-2">
          <Input
            type="number"
            min={0}
            value={quantityInput}
            onChange={(event) => setQuantityInput(event.target.value)}
            className="h-9 w-20"
            aria-label={`Stock for ${product.name}`}
          />
          <Button
            size="sm"
            variant="outline"
            disabled={pending}
            onClick={updateStock}
          >
            Update
          </Button>
        </div>
        <p className="mt-1 text-label-sm text-on-surface-variant">
          {stock.availableQuantity} available
        </p>
      </td>
      <td className="px-6 py-4">
        {(product.status === "ACTIVE" || product.status === "DRAFT") && (
          <Button
            size="sm"
            variant="outline"
            disabled={pending}
            onClick={togglePublish}
          >
            {product.status === "ACTIVE" ? "Delist" : "Publish"}
          </Button>
        )}
        {error && <p className="mt-1 text-label-sm text-error">{error}</p>}
      </td>
    </tr>
  );
}
