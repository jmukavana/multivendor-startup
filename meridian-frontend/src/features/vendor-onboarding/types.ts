import type { IsoInstant, Uuid } from "@/shared/api";
import type {
  VendorApplicationField,
  VendorApplicationValues,
} from "./schemas";

/** Mirrors `vendor.VendorApplicationStatus`. */
export type VendorApplicationStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "INFORMATION_REQUESTED"
  | "APPROVED"
  | "REJECTED"
  | "SUSPENDED";

/**
 * Mirrors `vendor.VendorApplication` in full.
 *
 * The application outlives approval — `vendorId` is filled in once it is approved, and is the
 * only link from the application a person submitted to the vendor they now trade as.
 */
export interface VendorApplication {
  readonly id: Uuid;
  readonly accountId: Uuid;
  readonly businessName: string;
  readonly registrationNumber: string | null;
  readonly kraPin: string;
  readonly physicalAddress: string;
  readonly county: string;
  readonly contactName: string;
  readonly contactPhone: string;
  readonly contactEmail: string | null;
  readonly status: VendorApplicationStatus;
  readonly rejectionReason: string | null;
  /** BR-VND-04: what an admin has asked the applicant to supply before review continues. */
  readonly informationRequest: string | null;
  readonly submittedAt: IsoInstant | null;
  readonly vendorId: Uuid | null;
}

/** Mirrors `VerificationDocumentEntity.DocumentType`. */
export type VerificationDocumentType =
  | "NATIONAL_ID"
  | "BUSINESS_REGISTRATION_CERTIFICATE"
  | "KRA_PIN_CERTIFICATE";

export type VendorApplicationState =
  | { readonly status: "idle" }
  | {
      readonly status: "error";
      readonly message: string;
      /** Shown alongside the message so support can find the request in the logs. */
      readonly traceId?: string;
      readonly fieldErrors: Partial<Record<VendorApplicationField, string[]>>;
      readonly values: VendorApplicationValues;
    };

export const IDLE_VENDOR_APPLICATION_STATE: VendorApplicationState = {
  status: "idle",
};
