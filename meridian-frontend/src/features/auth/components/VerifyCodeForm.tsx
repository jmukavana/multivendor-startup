"use client";

import { useActionState } from "react";

import { FormAlert, SubmitButton, TextField } from "@/shared/components/ui";
import { verifySignInCode } from "../actions";
import { type AuthState, IDLE_AUTH_STATE } from "../types";

interface VerifyCodeFormProps {
  readonly identifier: string;
  readonly next: string;
  readonly onStartOver: () => void;
}

function errorsFor(state: AuthState, field: "code") {
  return state.status === "error" ? state.fieldErrors[field] : undefined;
}

export function VerifyCodeForm({
  identifier,
  next,
  onStartOver,
}: VerifyCodeFormProps) {
  const [state, formAction, pending] = useActionState(
    verifySignInCode,
    IDLE_AUTH_STATE,
  );

  return (
    <form action={formAction} className="flex flex-col gap-5">
      <input type="hidden" name="identifier" value={identifier} />
      <input type="hidden" name="next" value={next} />

      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          Enter your code
        </h1>
        <p className="mt-2 text-sm opacity-70">
          If <span className="font-medium">{identifier}</span> matches an
          account, we have sent it a 6-digit code. It expires in 10 minutes.
        </p>
      </div>

      {state.status === "error" && (
        <FormAlert message={state.message} traceId={state.traceId} />
      )}

      <TextField
        name="code"
        label="6-digit code"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        autoFocus
        errors={errorsFor(state, "code")}
        defaultValue={state.status === "error" ? (state.values.code ?? "") : ""}
      />

      <SubmitButton pending={pending} pendingLabel="Verifying…">
        Verify and continue
      </SubmitButton>

      <button
        type="button"
        onClick={onStartOver}
        className="text-sm underline underline-offset-4 opacity-70"
      >
        Use a different number or email
      </button>
    </form>
  );
}
