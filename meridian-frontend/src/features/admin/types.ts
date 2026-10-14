import type { IsoInstant, Money, Uuid } from "@/shared/api";

/** Mirrors `admin.AuditLogEntry`. `entityId` is a string, not a UUID — not every entity has one. */
export interface AuditLogEntry {
  readonly id: Uuid;
  readonly actorAccountId: Uuid | null;
  readonly action: string;
  readonly entityType: string;
  readonly entityId: string;
  readonly detail: string | null;
  readonly occurredAt: IsoInstant;
}

export type RestrictionStatus = "ACTIVE" | "LIFTED";

/** Mirrors `integrity.Restriction`. */
export interface Restriction {
  readonly id: Uuid;
  readonly scope: string;
  readonly key: string;
  readonly reason: string;
  readonly status: RestrictionStatus;
  readonly createdAt: IsoInstant;
  readonly liftedAt: IsoInstant | null;
  readonly liftedBy: Uuid | null;
  readonly liftReason: string | null;
}

/**
 * Mirrors `integrity.BuyerAbuseSignal`. The rates are BigDecimal on the wire, so they arrive
 * as JSON numbers between 0 and 1 — multiply for a percentage, do not treat them as one.
 */
export interface BuyerAbuseSignal {
  readonly buyerAccountId: Uuid;
  readonly cancellationRate: number;
  readonly returnRate: number;
  readonly disputeRate: number;
  /** Already over the configured threshold — the queue's reason for showing the row. */
  readonly flagged: boolean;
}

/** Mirrors `integrity.VendorFraudSignal`. */
export interface VendorFraudSignal {
  readonly vendorId: Uuid;
  readonly dispatchFailureRate: number;
  readonly payoutRecentlyChanged: boolean;
  readonly flagged: boolean;
}

/** Mirrors `OversellAttemptView` — BR-SEC-06's refused reservations. */
export interface OversellAttempt {
  readonly id: Uuid;
  readonly productId: Uuid;
  readonly requestedQuantity: number;
  readonly availableQuantity: number;
  readonly referenceId: Uuid;
  readonly occurredAt: IsoInstant;
}

/**
 * Mirrors `pricing.CommissionRate`. Configuration is versioned rather than edited, so a rate
 * carries the id of the version an order was priced under.
 */
export interface CommissionRate {
  readonly configurationVersionId: Uuid;
  readonly rate: number;
}

/** Mirrors `pricing.HoldbackRate`. */
export interface HoldbackRate {
  readonly configurationVersionId: Uuid;
  readonly rate: number;
}

export type TaxDocumentStatus =
  | "PENDING"
  | "TRANSMITTED"
  | "FAILED"
  | "DEAD_LETTERED";

/** Mirrors `tax.TaxInvoice`. */
export interface TaxInvoice {
  readonly id: Uuid;
  readonly parentOrderId: Uuid;
  readonly invoiceNumber: string;
  readonly amount: Money;
  readonly status: TaxDocumentStatus;
  readonly acknowledgementReference: string | null;
  readonly attemptCount: number;
  readonly createdAt: IsoInstant;
  readonly transmittedAt: IsoInstant | null;
}

/** Mirrors `tax.CreditNote`. */
export interface CreditNote {
  readonly id: Uuid;
  readonly taxInvoiceId: Uuid;
  readonly parentOrderId: Uuid;
  readonly amount: Money;
  readonly reason: string;
  readonly status: TaxDocumentStatus;
  readonly acknowledgementReference: string | null;
  readonly attemptCount: number;
  readonly createdAt: IsoInstant;
  readonly transmittedAt: IsoInstant | null;
}

/**
 * What an admin may know about an impersonation session they just started.
 *
 * Deliberately excludes the token `IssuedImpersonationSession` carries. That is a credential;
 * `impersonateVendor` puts it straight into the httpOnly session cookie and never hands it
 * back, so it cannot be logged, rendered, or forwarded by accident.
 */
export interface ImpersonationHandover {
  readonly targetAccountId: Uuid;
  readonly impersonatorAccountId: Uuid;
  readonly expiresAt: IsoInstant;
}

/**
 * Query for `GET /admin/orders/search`. Every field is optional; all of them narrow.
 *
 * A type alias rather than an interface on purpose — only an alias gets TypeScript's implicit
 * index signature, which is what lets it be passed straight to the client's `query` option.
 */
export type AdminOrderSearchQuery = {
  readonly reference?: string;
  readonly buyerAccountId?: Uuid;
  readonly paymentReference?: string;
  readonly sourceAddress?: string;
};
