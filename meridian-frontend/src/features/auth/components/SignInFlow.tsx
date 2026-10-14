"use client";

import { useActionState, useState } from "react";

import { FormAlert, SubmitButton, TextField } from "@/shared/components/ui";
import { registerAccount, requestSignInCode } from "../actions";
import type { AuthField } from "../schemas";
import { type AuthState, IDLE_AUTH_STATE } from "../types";
import { VerifyCodeForm } from "./VerifyCodeForm";

type Mode = "signin" | "register";

function errorsFor(state: AuthState, field: AuthField) {
  return state.status === "error" ? state.fieldErrors[field] : undefined;
}

function priorValue(state: AuthState, field: AuthField) {
  return state.status === "error" ? (state.values[field] ?? "") : "";
}

export function SignInFlow({ next }: { readonly next: string }) {
  const [mode, setMode] = useState<Mode>("signin");
  const [signInState, signInAction, signingIn] = useActionState(
    requestSignInCode,
    IDLE_AUTH_STATE,
  );
  const [registerState, registerAction, registering] = useActionState(
    registerAccount,
    IDLE_AUTH_STATE,
  );

  const isRegister = mode === "register";
  const state = isRegister ? registerState : signInState;
  const pending = isRegister ? registering : signingIn;

  if (state.status === "code-sent") {
    return (
      <VerifyCodeForm
        identifier={state.identifier}
        next={next}
        onStartOver={() => setMode(isRegister ? "signin" : "register")}
      />
    );
  }

  return (
    <form
      action={isRegister ? registerAction : signInAction}
      className="flex flex-col gap-5"
    >
      <div>
        <h1 className="text-2xl font-semibold tracking-tight">
          {isRegister ? "Create your account" : "Sign in to Meridian"}
        </h1>
        <p className="mt-2 text-sm opacity-70">
          We will send a 6-digit code to confirm it is you. No password needed.
        </p>
      </div>

      {state.status === "error" && (
        <FormAlert message={state.message} traceId={state.traceId} />
      )}

      {isRegister && (
        <TextField
          name="displayName"
          label="Your name"
          autoComplete="name"
          errors={errorsFor(state, "displayName")}
          defaultValue={priorValue(state, "displayName")}
        />
      )}

      <TextField
        name="identifier"
        label="Phone number or email"
        inputMode="tel"
        autoComplete="username"
        hint="Kenyan mobile, e.g. 0712345678."
        errors={errorsFor(state, "identifier")}
        defaultValue={priorValue(state, "identifier")}
      />

      <SubmitButton pending={pending} pendingLabel="Sending code…">
        {isRegister ? "Create account" : "Send me a code"}
      </SubmitButton>

      <button
        type="button"
        onClick={() => setMode(isRegister ? "signin" : "register")}
        className="text-sm underline underline-offset-4 opacity-70"
      >
        {isRegister ? "I already have an account" : "Create an account instead"}
      </button>
    </form>
  );
}
