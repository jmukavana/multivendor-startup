package com.jamlapps.meridian.iam.internal;

/**
 * Produces the plaintext of a one-time code.
 *
 * <p>A seam, not an abstraction the domain needs: production has exactly one implementation. It
 * exists so a test can substitute a generator that records what it produced, without the
 * plaintext ever being reachable from {@link AuthenticationService} in any other way — no log
 * statement, no event field, no repository column holds it. See ADR-008.
 */
interface OneTimeCodeGenerator {

    String generate();
}
