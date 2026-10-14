import { z } from "zod";

import { KE_MOBILE } from "@/shared/lib/phone";

/** KRA PIN: leading class letter, nine digits, checksum letter. BR-VND-02. */
const KRA_PIN = /^[AP]\d{9}[A-Z]$/;

export const vendorApplicationSchema = z.object({
  businessName: z
    .string({ error: "Business name is required" })
    .min(2, { error: "Business name is too short" })
    .max(120, { error: "Business name must be 120 characters or fewer" }),

  /** Absent for sole traders — UC-01 A2 puts them on the reduced verification tier. */
  registrationNumber: z
    .string()
    .max(50, { error: "Registration number must be 50 characters or fewer" })
    .optional(),

  kraPin: z
    .string({ error: "KRA PIN is required" })
    .regex(KRA_PIN, { error: "KRA PIN must look like A123456789Z" }),

  physicalAddress: z
    .string({ error: "Physical address is required" })
    .min(5, { error: "Physical address is too short" })
    .max(200, { error: "Physical address must be 200 characters or fewer" }),

  county: z
    .string({ error: "County is required" })
    .min(2, { error: "County is required" }),

  contactName: z
    .string({ error: "Contact name is required" })
    .min(2, { error: "Contact name is too short" })
    .max(120, { error: "Contact name must be 120 characters or fewer" }),

  contactPhone: z
    .string({ error: "Contact phone is required" })
    .regex(KE_MOBILE, {
      error: "Enter a Kenyan mobile number, e.g. 0712345678",
    }),

  contactEmail: z.email({ error: "Enter a valid email address" }).optional(),
});

export type VendorApplicationInput = z.infer<typeof vendorApplicationSchema>;

export const VENDOR_APPLICATION_FIELDS = [
  "businessName",
  "registrationNumber",
  "kraPin",
  "physicalAddress",
  "county",
  "contactName",
  "contactPhone",
  "contactEmail",
] as const;

export type VendorApplicationField = (typeof VENDOR_APPLICATION_FIELDS)[number];

/** An empty text input arrives as "", which is absent rather than invalid. */
function field(formData: FormData, name: string): string | undefined {
  const value = formData.get(name);
  if (typeof value !== "string") return undefined;
  const trimmed = value.trim();
  return trimmed === "" ? undefined : trimmed;
}

/** Raw strings echoed back to the form so a rejected submission is not retyped. */
export type VendorApplicationValues = Partial<
  Record<VendorApplicationField, string>
>;

export function readVendorApplicationForm(
  formData: FormData,
): VendorApplicationValues {
  const values: VendorApplicationValues = {};
  for (const name of VENDOR_APPLICATION_FIELDS) {
    const value = field(formData, name);
    if (value !== undefined) {
      values[name] = name === "kraPin" ? value.toUpperCase() : value;
    }
  }
  return values;
}

export const uploadDocumentSchema = z.object({
  applicationId: z.uuid(),
  documentType: z.enum(
    ["NATIONAL_ID", "BUSINESS_REGISTRATION_CERTIFICATE", "KRA_PIN_CERTIFICATE"],
    { error: "Choose which document this is" },
  ),
});

export type UploadDocumentInput = z.infer<typeof uploadDocumentSchema>;
