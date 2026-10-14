"use server";

import { refresh } from "next/cache";
import { redirect } from "next/navigation";
import { z } from "zod";

import {
  type ActionResult,
  API_ERROR_CODES,
  api,
  attempt,
  failed,
  getErrorMessage,
  isApiError,
  parseInput,
} from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import { toE164 } from "@/shared/lib/phone";
import {
  readVendorApplicationForm,
  uploadDocumentSchema,
  type VendorApplicationField,
  type VendorApplicationValues,
  vendorApplicationSchema,
} from "./schemas";
import type { VendorApplication, VendorApplicationState } from "./types";

const APPLY_PATH = "/vendor/apply";

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
 * Server Actions are reachable by direct POST, not only through this form, so authentication
 * and validation are enforced here rather than in the UI.
 */
export async function submitVendorApplication(
  _previous: VendorApplicationState,
  formData: FormData,
): Promise<VendorApplicationState> {
  await requireSession(APPLY_PATH);

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

    // E3 — the PIN already belongs to a vendor or a live application. The backend checks this
    // before the insert and again on the unique index, so it can arrive either way.
    if (error.code === API_ERROR_CODES.KRA_PIN_TAKEN) {
      return failure(
        "This KRA PIN is already registered to a vendor or a pending application.",
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

    // Only an explicitly expired session ends the session. A bare 401 or a 403 can mean a
    // permission the caller lacks or a backend hiccup, and bouncing those to /login signs
    // people out while their cookies are still valid.
    if (error.code === API_ERROR_CODES.SESSION_EXPIRED) {
      redirect(`/login?next=${encodeURIComponent(APPLY_PATH)}`);
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

  // The application is a per-account read and never cached, so there is no tag to clear.
  refresh();
  // redirect throws a control-flow exception — it must stay outside the try block.
  redirect(`/vendor/apply/submitted?id=${created.id}`);
}

/** The backend caps verification documents at 10MB. */
const MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;

/**
 * BR-VND-03: attaches a verification document to an application.
 *
 * `documentType` rides in the query string, not the body — the backend reads it as a
 * `@RequestParam` alongside the multipart file part.
 */
export async function uploadVerificationDocument(
  input: unknown,
  formData: FormData,
): Promise<ActionResult<void>> {
  await requireSession(APPLY_PATH);

  const parsed = parseInput(uploadDocumentSchema, input);
  if (!parsed.ok) return parsed.failure;

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return failed("Choose a document to upload.", "VALIDATION_ERROR", {
      file: ["Choose a document to upload"],
    });
  }
  if (file.size > MAX_DOCUMENT_BYTES) {
    return failed("That document is larger than 10MB.", "VALIDATION_ERROR", {
      file: ["The document must be 10MB or smaller"],
    });
  }

  const upload = new FormData();
  upload.set("file", file);

  const { applicationId, documentType } = parsed.data;
  const result = await attempt("We could not upload that document.", () =>
    api.post<void>(`/vendors/applications/${applicationId}/documents`, upload, {
      query: { documentType },
    }),
  );

  if (result.status === "success") refresh();
  return result;
}
