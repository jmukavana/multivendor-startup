package com.jamlapps.meridian.iam;

import java.util.Optional;
import java.util.UUID;

/**
 * How other modules ask about accounts.
 *
 * <p>Read-only by design. The vendor module needs to know who an applicant is; it has no business
 * changing their roles. Anything that mutates an account is driven from inside {@code iam}, either
 * by its own endpoints or in response to an event.
 */
public interface Accounts {

    Optional<Account> findById(UUID accountId);

    boolean exists(UUID accountId);
}
