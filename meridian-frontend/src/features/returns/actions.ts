"use server";

import { refresh } from "next/cache";

import {
  type ActionResult,
  api,
  attempt,
  failed,
  parseInput,
} from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import {
  requestReturnSchema,
  returnIdSchema,
  vendorResponseSchema,
} from "./schemas";
import type { Return } from "./types";

/** BR-RTN-01/02/04: the buyer names the items; the backend prices the refund. */
export async function requestReturn(
  input: unknown,
): Promise<ActionResult<Return>> {
  await requireSession("/orders");

  const parsed = parseInput(requestReturnSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not open that return.", () =>
    api.post<Return>("/returns", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}

/**
 * BR-RTN-05: the vendor accepts or refuses. A refusal escalates the return into a dispute for
 * finance to resolve — it does not end it, so the copy should not read like a rejection.
 */
export async function respondToReturn(
  input: unknown,
): Promise<ActionResult<Return>> {
  await requireSession("/vendor/returns");

  const parsed = parseInput(vendorResponseSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { returnId, accept } = parsed.data;
  const result = await attempt("We could not record that response.", () =>
    api.post<Return>(`/returns/${returnId}/vendor-response`, { accept }),
  );

  if (result.status === "success") refresh();
  return result;
}

/** The backend caps return evidence at 5MB per image. */
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

/** Photographic evidence for the return, which is what a dispute is decided on. */
export async function addReturnImage(
  returnId: string,
  formData: FormData,
): Promise<ActionResult<void>> {
  await requireSession("/orders");

  const parsed = parseInput(returnIdSchema, { returnId });
  if (!parsed.ok) return parsed.failure;

  const file = formData.get("file");
  if (!(file instanceof File) || file.size === 0) {
    return failed("Choose a photo to upload.", "VALIDATION_ERROR", {
      file: ["Choose a photo to upload"],
    });
  }
  if (file.size > MAX_IMAGE_BYTES) {
    return failed("That photo is larger than 5MB.", "VALIDATION_ERROR", {
      file: ["The photo must be 5MB or smaller"],
    });
  }

  const upload = new FormData();
  upload.set("file", file);

  const result = await attempt("We could not upload that photo.", () =>
    api.post<void>(`/returns/${parsed.data.returnId}/images`, upload),
  );

  if (result.status === "success") refresh();
  return result;
}
