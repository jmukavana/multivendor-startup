package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;

/**
 * A role held by an account, with when it was granted.
 *
 * <p>An embeddable rather than a bare {@code Role} in the collection: {@code account_role.granted_at}
 * is {@code NOT NULL} in V2, because a role grant is itself an event worth dating for the audit
 * trail BR-IAM-06 requires. A plain {@code Set<Role>} element collection has no column for that and
 * Hibernate would insert null, failing the constraint — which is what surfaced this.
 */
@Embeddable
class RoleGrant {

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "granted_at", nullable = false)
    private Instant grantedAt;

    protected RoleGrant() {
        // for JPA
    }

    RoleGrant(Role role, Instant grantedAt) {
        this.role = role;
        this.grantedAt = grantedAt;
    }

    Role role() {
        return role;
    }

    @Override
    public boolean equals(Object obj) {
        // Equality by role alone, deliberately: this backs a Set, and the invariant is one grant
        // per role per account — matching the composite primary key in V2 — not one grant per
        // (role, timestamp) pair, which would let the same role be added twice at different times.
        return obj instanceof RoleGrant other && role == other.role;
    }

    @Override
    public int hashCode() {
        return role == null ? 0 : role.hashCode();
    }
}
