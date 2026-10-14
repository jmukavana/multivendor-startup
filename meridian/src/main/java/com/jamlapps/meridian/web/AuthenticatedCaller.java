package com.jamlapps.meridian.web;

import java.util.Set;
import java.util.UUID;

/**
 * The authenticated caller, as any controller in any module can see it via
 * {@code @AuthenticationPrincipal AuthenticatedCaller}.
 *
 * <p>Deliberately minimal and free of any module-specific shape. Every module with an
 * authenticated endpoint needs to know who is calling and what they are allowed to do; almost none
 * of them need the full account record behind that identity. A module that does — showing a
 * buyer their own profile, say — asks {@code iam.Accounts} for it using the {@link #accountId()}
 * here, which is the same read-only, ask-the-owning-module pattern every other cross-module lookup
 * in this codebase already follows.
 *
 * <p>{@code authorities} are the raw Spring Security authority strings (for example
 * {@code "ROLE_FINANCE_ADMIN"}), not {@code iam.Role} — {@code web} cannot depend on {@code iam},
 * and does not need to: {@code hasAuthority} is enough for the checks a generic controller makes,
 * and {@code @PreAuthorize("hasRole(...)")} needs exactly this string form regardless.
 */
public record AuthenticatedCaller(UUID accountId, Set<String> authorities) {

    public AuthenticatedCaller {
        authorities = Set.copyOf(authorities);
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }
}
