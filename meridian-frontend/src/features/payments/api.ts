import "server-only";

import { api } from "@/shared/api";
import type { PaymentTransaction } from "./types";

/**
 * BR-PAY-03: where a pending payment has got to.
 *
 * Poll this after checkout — the order stays PENDING_PAYMENT until the provider's callback
 * lands, which the buyer's browser never sees.
 *
 * There is deliberately no client for `POST /payments/callback`: that endpoint exists for the
 * payment provider's webhook, is permitted unauthenticated for exactly that reason, and
 * anything the frontend posted to it would be a forged confirmation.
 */
export async function getPaymentTransaction(
  paymentTransactionId: string,
): Promise<PaymentTransaction> {
  return api.get<PaymentTransaction>(`/payments/${paymentTransactionId}`);
}
