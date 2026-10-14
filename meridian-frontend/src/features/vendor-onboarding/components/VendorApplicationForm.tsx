"use client";

import { useActionState } from "react";

import { FormAlert, SubmitButton, TextField } from "@/shared/components/ui";
import { submitVendorApplication } from "../actions";
import type { VendorApplicationField } from "../schemas";
import {
  IDLE_VENDOR_APPLICATION_STATE,
  type VendorApplicationState,
} from "../types";

interface VendorApplicationFormProps {
  readonly submissionId: string;
}

function errorsFor(
  state: VendorApplicationState,
  field: VendorApplicationField,
) {
  return state.status === "error" ? state.fieldErrors[field] : undefined;
}

function priorValue(
  state: VendorApplicationState,
  field: VendorApplicationField,
) {
  return state.status === "error" ? (state.values[field] ?? "") : "";
}

export function VendorApplicationForm({
  submissionId,
}: VendorApplicationFormProps) {
  const [state, formAction, pending] = useActionState(
    submitVendorApplication,
    IDLE_VENDOR_APPLICATION_STATE,
  );

  const field = (name: VendorApplicationField) => ({
    name,
    errors: errorsFor(state, name),
    defaultValue: priorValue(state, name),
  });

  return (
    <form action={formAction} className="flex flex-col gap-5">
      {/* Replayed on every retry so a double submit cannot create two applications. */}
      <input type="hidden" name="submissionId" value={submissionId} />

      {state.status === "error" && (
        <FormAlert message={state.message} traceId={state.traceId} />
      )}

      <TextField
        {...field("businessName")}
        label="Business name"
        autoComplete="organization"
      />
      <TextField
        {...field("registrationNumber")}
        label="Business registration number"
        optional
        hint="Leave blank if you are registering as a sole trader."
      />
      <TextField
        {...field("kraPin")}
        label="KRA PIN"
        hint="Eleven characters, e.g. A123456789Z."
      />
      <TextField
        {...field("physicalAddress")}
        label="Physical address"
        autoComplete="street-address"
      />
      <TextField
        {...field("county")}
        label="County"
        autoComplete="address-level1"
      />
      <TextField
        {...field("contactName")}
        label="Contact name"
        autoComplete="name"
      />
      <TextField
        {...field("contactPhone")}
        label="Contact phone"
        type="tel"
        inputMode="tel"
        autoComplete="tel"
        hint="Kenyan mobile, e.g. 0712345678."
      />
      <TextField
        {...field("contactEmail")}
        label="Contact email"
        type="email"
        inputMode="email"
        optional
        autoComplete="email"
      />

      <SubmitButton pending={pending} pendingLabel="Submitting…">
        Submit application
      </SubmitButton>
    </form>
  );
}
