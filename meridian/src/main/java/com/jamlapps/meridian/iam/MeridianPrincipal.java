package com.jamlapps.meridian.iam;

import java.util.UUID;

/**
 * The authenticated caller, as any module can see it.
 *
 * <p>Obtained from {@code SecurityContextHolder.getContext().getAuthentication().getPrincipal()}.
 * Carries no credential: the token that produced it is not reachable from here, so a module cannot
 * accidentally log or forward it.
 */
public record MeridianPrincipal(UUID accountId, Account account) {

    public boolean hasRole(Role role) {
        return account.hasRole(role);
    }
}
