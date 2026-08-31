"use client";

import { useActionState, useId } from "react";

import { submitVendorApplication } from "../actions";
import type { VendorApplicationField } from "../schemas";
import {
  IDLE_VENDOR_APPLICATION_STATE,
  type VendorApplicationState,
} from "../types";

interface FieldProps {
  readonly name: VendorApplicationField;
  readonly label: string;
  readonly state: VendorApplicationState;
  readonly type?: string;
  readonly hint?: string;
  readonly optional?: boolean;
  readonly autoComplete?: string;
}

function Field({
  name,
  label,
  state,
  type = "text",
  hint,
  optional = false,
  autoComplete,
}: FieldProps) {
  const id = useId();
  const errors = state.status === "error" ? state.fieldErrors[name] : undefined;
  const describedBy = [
    hint ? `${id}-hint` : null,
    errors ? `${id}-error` : null,
  ]
    .filter(Boolean)
    .join(" ");

  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={id} className="text-sm font-medium">
        {label}
        {optional && (
          <span className="ml-1 font-normal opacity-60">(optional)</span>
        )}
      </label>
      <input
        id={id}
        name={name}
        type={type}
        autoComplete={autoComplete}
        defaultValue={
          state.status === "error" ? (state.values[name] ?? "") : ""
        }
        aria-invalid={errors ? true : undefined}
        aria-describedby={describedBy || undefined}
        className="rounded-md border border-black/15 bg-transparent px-3 py-2 text-sm outline-none focus-visible:border-foreground aria-[invalid=true]:border-red-600 dark:border-white/20"
      />
      {hint && (
        <p id={`${id}-hint`} className="text-xs opacity-60">
          {hint}
        </p>
      )}
      {errors && (
        <p id={`${id}-error`} className="text-xs text-red-600">
          {errors.join(". ")}
        </p>
      )}
    </div>
  );
}

interface VendorApplicationFormProps {
  readonly submissionId: string;
}

export function VendorApplicationForm({
  submissionId,
}: VendorApplicationFormProps) {
  const [state, formAction, pending] = useActionState(
    submitVendorApplication,
    IDLE_VENDOR_APPLICATION_STATE,
  );

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {/* Replayed on every retry so a double submit cannot create two applications. */}
      <input type="hidden" name="submissionId" value={submissionId} />

      {state.status === "error" && (
        <output className="block rounded-md border border-red-600/40 bg-red-600/10 px-3 py-2 text-sm text-red-700 dark:text-red-400">
          {state.message}
          {state.traceId && (
            <span className="mt-1 block font-mono text-xs opacity-70">
              Reference: {state.traceId}
            </span>
          )}
        </output>
      )}

      <Field
        name="businessName"
        label="Business name"
        state={state}
        autoComplete="organization"
      />
      <Field
        name="registrationNumber"
        label="Business registration number"
        state={state}
        optional
        hint="Leave blank if you are registering as a sole trader."
      />
      <Field
        name="kraPin"
        label="KRA PIN"
        state={state}
        hint="Eleven characters, e.g. A123456789Z."
      />
      <Field
        name="physicalAddress"
        label="Physical address"
        state={state}
        autoComplete="street-address"
      />
      <Field
        name="county"
        label="County"
        state={state}
        autoComplete="address-level1"
      />
      <Field
        name="contactName"
        label="Contact name"
        state={state}
        autoComplete="name"
      />
      <Field
        name="contactPhone"
        label="Contact phone"
        state={state}
        type="tel"
        autoComplete="tel"
        hint="Kenyan mobile, e.g. 0712345678."
      />
      <Field
        name="contactEmail"
        label="Contact email"
        state={state}
        type="email"
        optional
        autoComplete="email"
      />

      <button
        type="submit"
        disabled={pending}
        className="mt-2 rounded-md bg-foreground px-4 py-2.5 text-sm font-medium text-background disabled:opacity-50"
      >
        {pending ? "Submitting…" : "Submit application"}
      </button>
    </form>
  );
}
