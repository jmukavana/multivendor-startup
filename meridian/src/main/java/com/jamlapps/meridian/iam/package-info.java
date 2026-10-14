/**
 * Identity and Access.
 *
 * <p>Implements BR-IAM of the business requirements document. Delivered in phase 1.
 * Depends on no other module.
 *
 * <p><strong>{@code ImpersonationSessions} (BR-ADM-08) is the one piece of this module's public
 * API another module calls to mint a session, not just to read state.</strong> It reuses {@code
 * AuthenticationService}'s existing token-issuance mechanics end to end — same {@code
 * TokenHashing}, same {@code session_token} table, same auth-event trail — rather than a second,
 * parallel path to becoming authenticated as an account. {@code allowedDependencies} did not
 * change to support it: this module still depends on nothing but {@code web}, and does not need
 * to know what a vendor is to mint a session for one — see {@code vendor.package-info.java} and
 * {@code ImpersonationSessions}'s own javadoc for the caller-resolves-the-target half of this.
 *
 * <p><strong>This module no longer has an {@code events} sub-package.</strong> {@code
 * OneTimeCodeIssued} was its only event, and publishing it turned out to be a real bug: the event
 * deliberately never carried the plaintext one-time code (to avoid a credential at rest in the
 * event publication registry), but nothing downstream could resolve one back from a BCrypt hash
 * either, so the SMS a user actually received never contained a code to type back in — found by
 * running the real application, not by any test, since every test replaces code generation with
 * {@code CapturingOneTimeCodeGenerator} and never exercises delivery. {@code
 * AuthenticationService.issueCode} now calls {@code web.MessageGateway.sendOneTimeCode} directly,
 * synchronously, while the plaintext code is still in scope — see that interface's javadoc. This
 * module still publishes nothing and still depends on nothing but {@code web}; {@code
 * MessageGateway} is a slot {@code web} defines, the same direction every other foundational-module
 * capability in this codebase already flows.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Identity and Access",
        allowedDependencies = {"web"})
package com.jamlapps.meridian.iam;
