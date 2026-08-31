package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.Account;
import com.jamlapps.meridian.iam.AccountStatus;
import com.jamlapps.meridian.iam.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "account")
class AccountEntity {

    @Id
    private UUID id;

    @Column(name = "phone_number")
    private @Nullable String phoneNumber;

    private @Nullable String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status;

    // Eager because every authenticated request needs the roles to build the authentication, so a
    // lazy collection would be a second query on the hot path for no benefit. The set is at most
    // six rows.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "account_role", joinColumns = @JoinColumn(name = "account_id"))
    private Set<RoleGrant> roles = new HashSet<>();

    @Column(name = "phone_verified_at")
    private @Nullable Instant phoneVerifiedAt;

    @Column(name = "email_verified_at")
    private @Nullable Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AccountEntity() {
        // for JPA
    }

    AccountEntity(UUID id, @Nullable String phoneNumber, @Nullable String email, String displayName, Instant now) {
        this.id = id;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.displayName = displayName;
        this.status = AccountStatus.PENDING_VERIFICATION;
        this.roles = new HashSet<>(Set.of(new RoleGrant(Role.BUYER, now)));
        this.createdAt = now;
        this.updatedAt = now;
    }

    Account toAccount() {
        return new Account(id, phoneNumber, email, displayName, status, roleSet(), phoneVerifiedAt, emailVerifiedAt);
    }

    void markPhoneVerified(Instant now) {
        this.phoneVerifiedAt = now;
        activateIfPending(now);
    }

    void markEmailVerified(Instant now) {
        this.emailVerifiedAt = now;
        activateIfPending(now);
    }

    /**
     * A first verified identifier moves the account out of PENDING_VERIFICATION. Suspension and
     * closure are deliberately untouched: verifying an identifier must never resurrect a suspended
     * account, which is the obvious way an automated flow would undo an admin decision.
     */
    private void activateIfPending(Instant now) {
        if (status == AccountStatus.PENDING_VERIFICATION) {
            status = AccountStatus.ACTIVE;
        }
        this.updatedAt = now;
    }

    UUID id() {
        return id;
    }

    AccountStatus status() {
        return status;
    }

    Set<Role> roles() {
        return roleSet();
    }

    private Set<Role> roleSet() {
        return roles.stream().map(RoleGrant::role).collect(Collectors.toUnmodifiableSet());
    }

    @Nullable String phoneNumber() {
        return phoneNumber;
    }

    @Nullable String email() {
        return email;
    }
}
