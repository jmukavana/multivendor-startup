package com.jamlapps.meridian.iam;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * An account as other modules see it. A view, not the entity: nothing outside {@code iam} can
 * mutate an account by holding one.
 *
 * <p>Carries no credential material of any kind.
 */
public record Account(
        UUID id,
        @Nullable String phoneNumber,
        @Nullable String email,
        String displayName,
        AccountStatus status,
        Set<Role> roles,
        @Nullable Instant phoneVerifiedAt,
        @Nullable Instant emailVerifiedAt) {

    public Account {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    /**
     * BR-IAM-02: a verified phone number is the precondition for checkout. Email verification does
     * not substitute — the requirement is specific, because delivery and payment both run on the
     * phone number.
     */
    public boolean canCheckOut() {
        return status == AccountStatus.ACTIVE && phoneVerifiedAt != null;
    }
}
