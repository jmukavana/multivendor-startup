import type {
  VendorApplicationField,
  VendorApplicationValues,
} from "./schemas";

export type VendorApplicationStatus =
  | "DRAFT"
  | "SUBMITTED"
  | "UNDER_REVIEW"
  | "INFORMATION_REQUESTED"
  | "APPROVED"
  | "REJECTED"
  | "SUSPENDED";

export interface VendorApplication {
  readonly id: string;
  readonly status: VendorApplicationStatus;
  readonly businessName: string;
  readonly submittedAt: string | null;
}

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
