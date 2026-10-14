"use client";

import { useId } from "react";

interface TextFieldProps {
  readonly name: string;
  readonly label: string;
  readonly errors?: readonly string[];
  readonly defaultValue?: string;
  readonly type?: string;
  readonly hint?: string;
  readonly optional?: boolean;
  readonly autoComplete?: string;
  readonly inputMode?: "text" | "tel" | "email" | "numeric";
  readonly maxLength?: number;
  readonly autoFocus?: boolean;
}

export function TextField({
  name,
  label,
  errors,
  defaultValue = "",
  type = "text",
  hint,
  optional = false,
  autoComplete,
  inputMode,
  maxLength,
  autoFocus = false,
}: TextFieldProps) {
  const id = useId();
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
        inputMode={inputMode}
        maxLength={maxLength}
        autoComplete={autoComplete}
        // biome-ignore lint/a11y/noAutofocus: the sole field of a single-purpose step
        autoFocus={autoFocus}
        defaultValue={defaultValue}
        aria-invalid={errors ? true : undefined}
        aria-describedby={describedBy || undefined}
        className="rounded-md border border-black/15 bg-transparent px-3 py-2 text-sm outline-none dark:border-white/20"
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
