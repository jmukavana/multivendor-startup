/** Kenyan mobile in local (07…, 01…) or international (+2547…, +2541…) form. */
export const KE_MOBILE = /^(?:\+254|0)(?:1|7)\d{8}$/;

/**
 * DAT-04 normalises phone numbers to international format on capture, and the backend
 * distinguishes a phone identifier from an email one by the leading "+". A local 07… number
 * sent as typed would be looked up as an email address and never match an account.
 */
export function toE164(mobile: string): string {
  return mobile.startsWith("0") ? `+254${mobile.slice(1)}` : mobile;
}
