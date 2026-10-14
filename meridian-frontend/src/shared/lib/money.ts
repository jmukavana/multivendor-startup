import type { Money } from "@/shared/api";

/**
 * Renders a backend `Money` for display.
 *
 * The minor-unit divisor comes from the currency itself rather than a hardcoded 100 — Intl
 * knows that KES and USD have two decimal places while JPY has none, and guessing wrong
 * misprices everything on the page by two orders of magnitude.
 */
export function formatMoney(money: Money, locale = "en-KE"): string {
  const formatter = new Intl.NumberFormat(locale, {
    style: "currency",
    currency: money.currency,
  });
  const digits = formatter.resolvedOptions().maximumFractionDigits ?? 2;
  return formatter.format(money.minorUnits / 10 ** digits);
}

/** Parses a major-unit string a person typed ("1,250.50") into minor units. */
export function toMinorUnits(input: string, fractionDigits = 2): number | null {
  const normalised = input.replace(/[\s,]/g, "");
  if (!/^\d+(\.\d+)?$/.test(normalised)) return null;
  return Math.round(Number(normalised) * 10 ** fractionDigits);
}

/** The platform's home currency. Every price the backend quotes today is in KES. */
export const DEFAULT_CURRENCY = "KES";
