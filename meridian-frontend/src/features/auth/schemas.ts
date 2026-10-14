import { z } from "zod";

import { KE_MOBILE, toE164 } from "@/shared/lib/phone";

/**
 * Resolves a phone-or-email identifier to the form the backend expects: normalising to E.164 is
 * what actually distinguishes the two, since the backend tells them apart by a leading "+"
 * (DAT-04). Kept as a plain function rather than folded into the zod schema — the two branches
 * need genuinely different validation (a regex vs. `z.email()`) and a shared error message, which
 * reads more clearly as one function than as a zod union with two failure paths.
 */
export function normalizeIdentifier(raw: string): string | null {
  const value = raw.trim();
  if (KE_MOBILE.test(value)) return toE164(value);
  const email = z.email().safeParse(value.toLowerCase());
  return email.success ? email.data : null;
}

export const AUTH_FIELDS = ["identifier", "displayName", "code"] as const;
export type AuthField = (typeof AUTH_FIELDS)[number];

const identifierField = z
  .string({ error: "Enter your phone number or email address" })
  .min(1, { error: "Enter your phone number or email address" });

export const registerSchema = z.object({
  identifier: identifierField,
  displayName: z
    .string({ error: "Enter your name" })
    .min(2, { error: "Enter your name" })
    .max(120, { error: "Name must be 120 characters or fewer" }),
});

/** Sign-in has no displayName field — BR-IAM-01's registration is idempotent, but the silent
 * `/auth/code` endpoint this schema feeds does not accept one at all. */
export const signInSchema = z.object({
  identifier: identifierField,
});

export const verifyCodeSchema = z.object({
  identifier: z.string().min(1),
  code: z
    .string({ error: "Enter the 6-digit code" })
    .regex(/^\d{6}$/, { error: "Enter the 6-digit code" }),
});

/** An empty text input arrives as "", which is absent rather than invalid. */
function field(formData: FormData, name: string): string | undefined {
  const value = formData.get(name);
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

export function readAuthForm(
  formData: FormData,
): Partial<Record<AuthField, string>> {
  const values: Partial<Record<AuthField, string>> = {};
  for (const name of AUTH_FIELDS) {
    const value = field(formData, name);
    if (value !== undefined) values[name] = value;
  }
  return values;
}

/**
 * Validates a post-sign-in redirect target against an open redirect. `next` arrives from a
 * visitor-controlled search param and is later echoed into a hidden form field that a direct POST
 * could set to anything — only an absolute, same-origin path is safe to send a signed-in visitor
 * to.
 */
export function safeNextPath(
  raw: string | string[] | null | undefined,
): string {
  const value = Array.isArray(raw) ? raw[0] : raw;
  if (!value) return "/";
  // A scheme, a protocol-relative "//host" prefix, or a backslash (which several browsers
  // normalise to "/", defeating a same-origin check that only looks at forward slashes) are all
  // ways to smuggle an off-site destination past a naive "starts with /" check.
  if (
    !value.startsWith("/") ||
    value.startsWith("//") ||
    value.includes("\\")
  ) {
    return "/";
  }
  return value;
}
