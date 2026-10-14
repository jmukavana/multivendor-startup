package com.jamlapps.meridian.web;

/**
 * BR-SEC-01/02: a slot, not a feature — {@code web} is the OPEN, foundational module every other
 * module can already use without declaring a dependency, the same reasoning that already put
 * {@code AuthenticatedCaller} and {@code ApiException} here. {@code integrity} provides the real,
 * database-backed implementation; {@code iam}, {@code checkout} and {@code search} each call it at
 * the one endpoint BR-SEC-01 names for them, without any of the three gaining a dependency on
 * {@code integrity} — the same "define the slot where it's used, fill it from where the feature
 * lives" pattern {@code web.internal.SecurityConfiguration} already established for {@code
 * sessionAuthenticationFilter}, just inverted: there, a feature module fills a slot {@code web}
 * defines and wires into its own filter chain; here, a feature module ({@code integrity}) still
 * fills the slot, but three separate modules call it directly rather than {@code web} wiring it
 * into anything itself.
 *
 * <p><strong>An identifier-scoped and a source-address-scoped constant are separate, independently
 * configured rules</strong> — BR-SEC-01's "by account and by source address" is two knobs, not one
 * shared number split two ways. They have to be: a source address the test suite and, far more
 * consequentially, Kenyan mobile carrier CGNAT can put thousands of genuine distinct callers behind
 * is not the same signal as one phone number or account misbehaving, and a single threshold tight
 * enough to catch the latter would lock out everyone sharing the former's address. See {@code
 * integrity.internal.RateLimitProperties}'s defaults for exactly how much more permissive the
 * source-scoped rules are.
 *
 * <p>{@code scope} is one of the {@code SCOPE_*} constants below — a shared vocabulary between
 * caller and implementation, not an arbitrary string, so a typo fails loudly rather than silently
 * matching no configured rule. {@code key} is the caller's own identity for the check — a phone
 * number, an account id, a source address — {@code RateLimiter} does not interpret it.
 */
public interface RateLimiter {

    /** BR-SEC-02's "minimum interval between requests" — by phone number only; a shared source address has no bearing on it. */
    String SCOPE_OTC_ISSUANCE_INTERVAL = "OTC_ISSUANCE_INTERVAL";

    /** BR-SEC-02's "throttle issuance" — the phone-number half. */
    String SCOPE_OTC_ISSUANCE_VOLUME_IDENTIFIER = "OTC_ISSUANCE_VOLUME_IDENTIFIER";

    /** BR-SEC-01/02 — the source-address half, deliberately far more permissive. */
    String SCOPE_OTC_ISSUANCE_VOLUME_SOURCE = "OTC_ISSUANCE_VOLUME_SOURCE";

    /** BR-SEC-01: "authentication attempts" — the identifier half. */
    String SCOPE_AUTH_VERIFY_IDENTIFIER = "AUTH_VERIFY_IDENTIFIER";

    /** BR-SEC-01: "authentication attempts" — the source-address half. */
    String SCOPE_AUTH_VERIFY_SOURCE = "AUTH_VERIFY_SOURCE";

    /** BR-SEC-01: "checkout initiation" — the account half. */
    String SCOPE_CHECKOUT_INITIATION_ACCOUNT = "CHECKOUT_INITIATION_ACCOUNT";

    /** BR-SEC-01: "checkout initiation" — the source-address half. */
    String SCOPE_CHECKOUT_INITIATION_SOURCE = "CHECKOUT_INITIATION_SOURCE";

    /** BR-SEC-01: "search" — source address only; guest browsing (BR-IAM-08) often has no account to key on. */
    String SCOPE_SEARCH_SOURCE = "SEARCH_SOURCE";

    /**
     * @param scope one of the {@code SCOPE_*} constants
     * @param key the caller's identity for this check — never logged or persisted beyond what
     *     {@code integrity}'s own audit trail requires (BR-SEC-08)
     * @return true if this call is within the configured limit and counted against it, false if
     *     the limit is already exceeded and this call must be refused
     */
    boolean tryConsume(String scope, String key);
}
