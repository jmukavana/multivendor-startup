/**
 * Vendor Onboarding and Management.
 *
 * <p>Implements BR-VND of the business requirements document. Delivered in phase 2.
 * Allowed dependencies: iam.
 *
 * <p><strong>BR-ADM-08 (admin impersonation of a vendor account) is real, closing the last
 * deferred gap, and lives here rather than in {@code iam} or {@code admin}.</strong> {@code
 * iam.ImpersonationSessions} owns the actual session mechanics — {@code iam} already owns every
 * session, and duplicating that machinery elsewhere would be a second, parallel way to become
 * authenticated as an account, exactly the kind of thing this codebase's module boundaries exist
 * to prevent. What {@code iam} cannot do is decide WHO may be impersonated, because BR-ADM-08's
 * subject is specifically "a vendor account" and {@code iam} has no {@code vendor} dependency
 * (one would cycle, since {@code vendor} already depends on {@code iam}) and gains none for this.
 * {@code VendorImpersonationService} is the resolver: it turns a vendor id into that vendor's
 * owner account id and calls {@code ImpersonationSessions.issue}, the same "caller enforces its
 * own scope against a narrow, trusting interface" pattern {@code web.AuditLog} already
 * established. See {@code iam.ImpersonationSessions}'s javadoc for the session mechanics
 * themselves and for what this pass deliberately does not build (per-request attribution).
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Vendor Onboarding and Management",
        allowedDependencies = {"iam", "web", "shared"})
package com.jamlapps.meridian.vendor;
