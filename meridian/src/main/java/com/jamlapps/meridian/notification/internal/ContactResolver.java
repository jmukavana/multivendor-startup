package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.iam.Account;
import com.jamlapps.meridian.iam.Accounts;
import com.jamlapps.meridian.vendor.Vendor;
import com.jamlapps.meridian.vendor.Vendors;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Resolves an account or vendor id to a phone number and email — the one class in this module
 * that depends on {@code iam} and {@code vendor} beyond their event interfaces; see this module's
 * package doc for why that dependency is a deliberate superset of ADR-002's table, confined here.
 */
@Component
class ContactResolver {

    private final Accounts accounts;
    private final Vendors vendors;

    ContactResolver(Accounts accounts, Vendors vendors) {
        this.accounts = accounts;
        this.vendors = vendors;
    }

    Optional<Contact> forAccount(UUID accountId) {
        return accounts.findById(accountId).map(Contact::from);
    }

    /** Resolves the vendor's owning account — a vendor has no contact details of its own, only its owner's (BR-VND-01). */
    Optional<Contact> forVendor(UUID vendorId) {
        return vendors.findById(vendorId).map(Vendor::ownerAccountId).flatMap(this::forAccount);
    }

    record Contact(@Nullable String phoneNumber, @Nullable String email) {

        private static Contact from(Account account) {
            return new Contact(account.phoneNumber(), account.email());
        }
    }
}
