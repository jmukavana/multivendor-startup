"use server";

import { refresh } from "next/cache";

import { type ActionResult, api, attempt, parseInput } from "@/shared/api";
import { requireSession } from "@/shared/lib/guard";
import {
  holdVendorSchema,
  initiateRunSchema,
  releaseHoldSchema,
  runIdSchema,
  settlementCycleSchema,
} from "./schemas";
import type { SettlementCycle, SettlementRun } from "./types";

const SETTLEMENT_PATH = "/admin/settlement";

/** BR-SET-01: opens a draft run over everything currently payable in the currency. */
export async function initiateSettlementRun(
  input: unknown,
): Promise<ActionResult<SettlementRun>> {
  await requireSession(SETTLEMENT_PATH);

  const parsed = parseInput(initiateRunSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not start that settlement run.", () =>
    api.post<SettlementRun>("/settlement/runs", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}

/**
 * BR-SET-04: approval releases money.
 *
 * The backend disburses every pending line inside this one call, so it is slower than the
 * other actions here and is not safely retried on a timeout — the run it returns is the
 * authoritative record of what actually went out.
 */
export async function approveSettlementRun(
  input: unknown,
): Promise<ActionResult<SettlementRun>> {
  await requireSession(SETTLEMENT_PATH);

  const parsed = parseInput(runIdSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not approve that run.", () =>
    api.post<SettlementRun>(
      `/settlement/runs/${parsed.data.runId}/approve`,
      undefined,
      {
        // Disbursing every line takes longer than a default request should wait.
        timeoutMs: 60_000,
      },
    ),
  );

  if (result.status === "success") refresh();
  return result;
}

/** BR-SET-06: withholds a vendor's payouts until the hold is released. */
export async function holdVendorPayouts(
  input: unknown,
): Promise<ActionResult<void>> {
  await requireSession(SETTLEMENT_PATH);

  const parsed = parseInput(holdVendorSchema, input);
  if (!parsed.ok) return parsed.failure;

  const { vendorId, reason } = parsed.data;
  const result = await attempt("We could not place that hold.", () =>
    api.post<void>(`/settlement/vendors/${vendorId}/hold`, { reason }),
  );

  if (result.status === "success") refresh();
  return result;
}

export async function releaseVendorPayoutHold(
  input: unknown,
): Promise<ActionResult<void>> {
  await requireSession(SETTLEMENT_PATH);

  const parsed = parseInput(releaseHoldSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not release that hold.", () =>
    api.post<void>(`/settlement/holds/${parsed.data.holdId}/release`),
  );

  if (result.status === "success") refresh();
  return result;
}

/**
 * BR-SET-02: changes the payout cadence.
 *
 * Configuration is versioned rather than edited, so this creates a new version — orders
 * already settled keep the terms they were settled under.
 */
export async function setSettlementCycle(
  input: unknown,
): Promise<ActionResult<SettlementCycle>> {
  await requireSession(SETTLEMENT_PATH);

  const parsed = parseInput(settlementCycleSchema, input);
  if (!parsed.ok) return parsed.failure;

  const result = await attempt("We could not save that settlement cycle.", () =>
    api.post<SettlementCycle>("/settlement/config/cycle", parsed.data),
  );

  if (result.status === "success") refresh();
  return result;
}
