"use server";

import { updateTag } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";

import {
  API_ERROR_CODES,
  api,
  getErrorMessage,
  isApiError,
} from "@/shared/api";
import { getAccessToken } from "@/shared/lib/session";
import { VENDOR_APPLICATION_TAG } from "./constants";
import {
  readVendorApplicationForm,
  toE164,
  type VendorApplicationField,
  type VendorApplicationValues,
  vendorApplicationSchema,
} from "./schemas";
import type { VendorApplication, VendorApplicationState } from "./types";

function failure(
  message: string,
  values: VendorApplicationValues,
  fieldErrors: Partial<Record<VendorApplicationField, string[]>> = {},
  traceId?: string,
): VendorApplicationState {
  return { status: "error", message, fieldErrors, values, traceId };
}

/**
 * Submits a vendor application (UC-01 steps 2–5, BR-VND-01/02/04/05).
 *
 * Server Actions are reachable by direct POST, not only through this form, so
 * authentication and validation are enforced here rather than in the UI.
 */
export async function submitVendorApplication(
  _previous: VendorApplicationState,
  formData: FormData,
): Promise<VendorApplicationState> {
  if (!(await getAccessToken())) {
    redirect("/login?next=/vendor/apply");
  }

  const values = readVendorApplicationForm(formData);
  const parsed = vendorApplicationSchema.safeParse(values);

  if (!parsed.success) {
    const { fieldErrors } = z.flattenError(parsed.error);
    return failure(
      "Check the highlighted fields and try again.",
      values,
      fieldErrors,
    );
  }

  const submissionId = formData.get("submissionId");
  const application = parsed.data;

  let created: VendorApplication;
  try {
    created = await api.post<VendorApplication>(
      "/vendors/applications",
      { ...application, contactPhone: toE164(application.contactPhone) },
      {
        idempotencyKey:
          typeof submissionId === "string" ? submissionId : undefined,
      },
    );
  } catch (error) {
    if (!isApiError(error)) throw error;

    // E3 — the PIN already belongs to an approved vendor.
    if (error.code === API_ERROR_CODES.KRA_PIN_TAKEN) {
      return failure(
        "This KRA PIN is already registered to an approved vendor.",
        values,
        { kraPin: ["Already registered to another vendor"] },
        error.traceId,
      );
    }

    if (error.status === 400 || error.status === 422) {
      return failure(
        getErrorMessage(error, "Check the highlighted fields and try again."),
        values,
        error.fieldErrors as Partial<Record<VendorApplicationField, string[]>>,
        error.traceId,
      );
    }

    // Only an explicitly expired session ends the session. A bare 401 or a 403
    // can mean a permission the caller lacks or a backend hiccup, and bouncing
    // those to /login signs people out while their cookies are still valid.
    if (error.code === API_ERROR_CODES.SESSION_EXPIRED) {
      redirect("/login?next=/vendor/apply");
    }

    return failure(
      getErrorMessage(
        error,
        "We could not submit your application. Please try again shortly.",
      ),
      values,
      {},
      error.traceId,
    );
  }

  updateTag(VENDOR_APPLICATION_TAG);
  // redirect throws a control-flow exception — it must stay outside the try block.
  redirect(`/vendor/apply/submitted?id=${created.id}`);
}
