package com.jamlapps.meridian.iam;

import java.util.List;
import java.util.UUID;

/**
 * BR-ADM-08: a real, time-limited session an admin can obtain for a support purpose, acting with
 * another account's own identity and authorities — not a separate, parallel "admin as vendor"
 * code path duplicated elsewhere. {@code iam} already owns session issuance (see {@code
 * AuthenticationService}); this is the same mechanism, a different way in.
 *
 * <p>Deliberately does not decide WHO may be impersonated — {@code iam}'s {@code
 * allowedDependencies} has no {@code vendor} (adding one would cycle: {@code vendor} already
 * depends on {@code iam}) and does not need one. {@code
 * vendor.internal.AdminVendorImpersonationController} is the only caller today, resolving a
 * vendor id to that vendor's owner account id before calling {@link #issue}, and gating the
 * endpoint itself to {@code PLATFORM_ADMIN}/{@code SUPER_ADMIN} — this interface trusts its
 * caller on both counts, the same way {@code web.AuditLog} trusts every caller to describe its
 * own action truthfully rather than re-deriving it.
 *
 * <p><strong>What this does not do:</strong> attribute individual requests made through an
 * impersonation session back to the impersonating admin. Every module downstream sees an ordinary
 * {@code AuthenticatedCaller} for the target account once a session is issued — indistinguishable
 * from that account's own real session for the rest of the request's life. {@code
 * impersonatorAccountId} is recorded once, at issuance ({@link #historyFor}, and BR-ADM-07's
 * audit log), which is what BR-ADM-08 actually asks for — "impersonation... logged and visible to
 * the vendor" — not a per-request attribution system, which would touch every module's request
 * handling for a requirement the BRD marks Could-priority.
 */
public interface ImpersonationSessions {

    /** @throws com.jamlapps.meridian.web.ApiException NOT_FOUND if {@code targetAccountId} does not exist */
    IssuedImpersonationSession issue(UUID targetAccountId, UUID impersonatorAccountId, String reason);

    /** Most recent first. Empty if {@code targetAccountId} has never been impersonated. */
    List<ImpersonationRecord> historyFor(UUID targetAccountId);
}
