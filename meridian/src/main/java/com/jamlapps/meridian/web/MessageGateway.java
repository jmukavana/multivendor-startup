package com.jamlapps.meridian.web;

import java.time.Instant;

/**
 * BR-IAM-01/02, BR-SEC-02: delivers a one-time code's plaintext to its destination, synchronously,
 * in the same call that generated it — the one deliberate exception to ADR-004's "modules notify
 * each other by publishing domain events, never a direct call for notification" rule. The
 * alternative that rule would otherwise require — publishing the code on an event, so {@code
 * notification} can react to it asynchronously — means writing the plaintext into the event
 * publication registry table, a credential at rest for as long as that row survives; the {@code
 * iam.events.OneTimeCodeIssued} event this slot replaced avoided that by never carrying the code
 * at all, which meant, in practice, that the code was generated, hashed, and then discarded before
 * anything downstream ever saw it — nobody could actually receive it. See ADR-004's "Note added"
 * section on this interface for the fuller reasoning, and the same class of exception the earlier
 * {@code RateLimiter} and {@code AuditLog} slots already made for their own reasons.
 *
 * <p>A slot in {@code web}, the OPEN foundational module every module can already use — filled by
 * {@code notification.internal.MessageGatewayAdapter}, the same "define the slot where it's used,
 * fill it from where the feature lives" pattern those two slots established. {@code iam} calls
 * this directly and gains no dependency on {@code notification} to do it.
 *
 * <p>Deliberately narrow — one method, for exactly this one plaintext-must-not-be-persisted case —
 * rather than a general "send any message now" gateway, which would invite bypassing the
 * event-based convention ADR-004 still governs for everything else.
 */
public interface MessageGateway {

    /**
     * @throws RuntimeException if delivery fails; the caller's transaction (challenge creation)
     *     rolls back with it — a code nobody received has nothing worth retrying, unlike BR-NTF-05's
     *     durable retry story for already-delivered-but-unconfirmed transactional sends
     */
    void sendOneTimeCode(String destination, String code, Instant expiresAt);
}
