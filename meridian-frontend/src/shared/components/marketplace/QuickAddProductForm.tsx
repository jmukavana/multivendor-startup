"use client";

import { useActionState, useState } from "react";

import { createProduct } from "@/features/catalog/actions";
import type { Product } from "@/features/catalog/types";
import { type ActionState, IDLE_ACTION_STATE } from "@/shared/api/action-state";
import { FormAlert, SubmitButton, TextField } from "@/shared/components/ui";
import { DEFAULT_CURRENCY, toMinorUnits } from "@/shared/lib/money";

/**
 * `createProduct` takes a plain object, not `FormData` — this is the client-only adapter that
 * lets it drive `useActionState` the same way `submitVendorApplication` does directly.
 */
async function submitNewProduct(
  _previous: ActionState<Product>,
  formData: FormData,
): Promise<ActionState<Product>> {
  const name = String(formData.get("name") ?? "").trim();
  const description = String(formData.get("description") ?? "").trim();
  const priceMinorUnits = toMinorUnits(String(formData.get("price") ?? ""));

  if (priceMinorUnits === null) {
    return {
      status: "error",
      code: "VALIDATION_ERROR",
      message: "Check the highlighted fields and try again.",
      fieldErrors: { price: ["Enter a valid price, e.g. 1500.00"] },
    };
  }

  return createProduct({
    name,
    description: description || undefined,
    priceMinorUnits,
    currency: DEFAULT_CURRENCY,
  });
}

/** BR-CAT-01: a listing this creates starts as a DRAFT — publishing is a separate step this dashboard does not yet offer. */
export function QuickAddProductForm() {
  const [state, formAction, pending] = useActionState(
    submitNewProduct,
    IDLE_ACTION_STATE,
  );
  const [open, setOpen] = useState(false);

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="text-label-md font-semibold text-primary hover:underline"
      >
        + List a new product
      </button>
    );
  }

  if (state.status === "success") {
    return (
      <p className="text-label-md text-secondary">
        &ldquo;{state.data.name}&rdquo; was created as a draft. Publish it from
        your product list to make it orderable.
      </p>
    );
  }

  return (
    <form
      action={formAction}
      className="flex max-w-md flex-col gap-4 rounded-lg border border-outline-variant p-4"
    >
      {state.status === "error" && (
        <FormAlert message={state.message} traceId={state.traceId} />
      )}
      <TextField
        name="name"
        label="Product name"
        errors={state.status === "error" ? state.fieldErrors.name : undefined}
      />
      <TextField
        name="description"
        label="Description"
        optional
        errors={
          state.status === "error" ? state.fieldErrors.description : undefined
        }
      />
      <TextField
        name="price"
        label={`Price (${DEFAULT_CURRENCY})`}
        inputMode="text"
        hint="e.g. 1500.00"
        errors={state.status === "error" ? state.fieldErrors.price : undefined}
      />
      <div className="flex gap-3">
        <SubmitButton pending={pending} pendingLabel="Creating…">
          Create draft listing
        </SubmitButton>
        <button
          type="button"
          onClick={() => setOpen(false)}
          className="text-label-md text-on-surface-variant underline underline-offset-4"
        >
          Cancel
        </button>
      </div>
    </form>
  );
}
