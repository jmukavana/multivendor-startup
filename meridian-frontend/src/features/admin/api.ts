import "server-only";

import type { NotificationMessage } from "@/features/notifications";
import type { ParentOrder } from "@/features/orders";
import type { PaymentTransaction } from "@/features/payments";
import type { Dispute } from "@/features/returns";
import type { VendorApplication } from "@/features/vendor-onboarding";
import {
  api,
  apiFetchEnvelope,
  type PageRequest,
  type PaginatedResponse,
} from "@/shared/api";
import type {
  AdminOrderSearchQuery,
  AuditLogEntry,
  BuyerAbuseSignal,
  CreditNote,
  OversellAttempt,
  Restriction,
  TaxInvoice,
  VendorFraudSignal,
} from "./types";

/**
 * Every read in this file is role-gated by `@PreAuthorize` on the backend — PLATFORM_ADMIN or
 * SUPER_ADMIN for the operational queues, FINANCE_ADMIN or SUPER_ADMIN for anything touching
 * money. A FORBIDDEN here means the caller's role, not their session: do not sign them out
 * over it, show them a page they are allowed to see instead.
 *
 * None of it is cached. These responses are privileged by definition, and a cache entry keyed
 * only by URL would hand one admin's queue to the next caller.
 */

/** The audit trail, optionally narrowed to one entity. Both filters apply or neither does. */
export async function listAuditLog(entity?: {
  entityType: string;
  entityId: string;
}): Promise<AuditLogEntry[]> {
  return api.get<AuditLogEntry[]>("/admin/audit-log", {
    query: entity,
  });
}

/** Support's order lookup — by reference, buyer, payment reference, or origin address. */
export async function searchOrders(
  query: AdminOrderSearchQuery = {},
): Promise<ParentOrder[]> {
  return api.get<ParentOrder[]>("/admin/orders/search", { query });
}

export async function listBuyerAbuseSignals(): Promise<BuyerAbuseSignal[]> {
  return api.get<BuyerAbuseSignal[]>("/admin/integrity/buyer-abuse");
}

export async function listVendorFraudSignals(): Promise<VendorFraudSignal[]> {
  return api.get<VendorFraudSignal[]>("/admin/integrity/vendor-fraud");
}

export async function listActiveRestrictions(): Promise<Restriction[]> {
  return api.get<Restriction[]>("/admin/integrity/restrictions");
}

export async function listLiftedRestrictions(): Promise<Restriction[]> {
  return api.get<Restriction[]>("/admin/integrity/restrictions/lifted");
}

/** BR-SEC-06: reservations the stock ledger refused, newest first. */
export async function listOversellAttempts(): Promise<OversellAttempt[]> {
  return api.get<OversellAttempt[]>("/admin/inventory/oversell-attempts");
}

/** Messages that exhausted their retries — the notification dead-letter queue. */
export async function listNotificationExceptions(): Promise<
  NotificationMessage[]
> {
  return api.get<NotificationMessage[]>("/admin/notifications/exceptions");
}

/** Everything sent to one recipient, for answering "did they get the code?". */
export async function listNotificationsForRecipient(
  recipient: string,
): Promise<NotificationMessage[]> {
  return api.get<NotificationMessage[]>("/admin/notifications", {
    query: { recipient },
  });
}

/** Payments stuck somewhere they should not be — finance's reconciliation queue. */
export async function listPaymentExceptions(): Promise<PaymentTransaction[]> {
  return api.get<PaymentTransaction[]>("/admin/payments/exceptions");
}

/** BR-RTN-06: returns a vendor refused, waiting on a finance decision. */
export async function listDisputeQueue(): Promise<Dispute[]> {
  return api.get<Dispute[]>("/admin/returns/disputes");
}

export async function getOrderInvoice(
  parentOrderId: string,
): Promise<TaxInvoice> {
  return api.get<TaxInvoice>(`/admin/tax/orders/${parentOrderId}/invoice`);
}

export async function listOrderCreditNotes(
  parentOrderId: string,
): Promise<CreditNote[]> {
  return api.get<CreditNote[]>(
    `/admin/tax/orders/${parentOrderId}/credit-notes`,
  );
}

/** BR-TAX: documents KRA never acknowledged, which someone has to chase by hand. */
export async function listInvoiceExceptions(): Promise<TaxInvoice[]> {
  return api.get<TaxInvoice[]>("/admin/tax/invoices/exceptions");
}

export async function listCreditNoteExceptions(): Promise<CreditNote[]> {
  return api.get<CreditNote[]>("/admin/tax/credit-notes/exceptions");
}

/** BR-VND-04: the review queue, oldest submission first — it is a queue, not a listing. */
export async function listVendorApplicationQueue({
  page = 0,
  perPage = 20,
}: PageRequest = {}): Promise<PaginatedResponse<VendorApplication>> {
  return apiFetchEnvelope<PaginatedResponse<VendorApplication>>(
    "/admin/vendor-applications",
    { query: { page, perPage } },
  );
}
