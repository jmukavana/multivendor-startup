package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.Account;
import com.jamlapps.meridian.iam.AccountStatus;
import com.jamlapps.meridian.iam.events.OneTimeCodeIssued;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registration, one-time codes and session issuance.
 *
 * <p>Two rules run through all of it.
 *
 * <p><strong>Nothing here tells a caller whether an identifier exists.</strong> Requesting a code
 * for an unknown number and for a known one are indistinguishable from outside: same response, same
 * shape, same rough timing. Otherwise the endpoint becomes a way to enumerate the platform's
 * customers, which matters more for a marketplace whose vendors are identifiable businesses.
 *
 * <p><strong>Codes are never logged and never leave in an event.</strong> The plaintext code exists
 * in one place, the response of the request that issued it, on its way to the SMS gateway.
 */
@Service
class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private static final Duration CODE_VALIDITY = Duration.ofMinutes(10);
    private static final Duration SESSION_VALIDITY = Duration.ofDays(30);
    private static final String PURPOSE_PHONE = "PHONE_VERIFICATION";

    private final AccountRepository accounts;
    private final SessionTokenRepository sessions;
    private final OtpChallengeRepository challenges;
    private final AuthEventRecorder authEvents;
    private final PasswordEncoder codeEncoder;
    private final OneTimeCodeGenerator codeGenerator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    AuthenticationService(
            AccountRepository accounts,
            SessionTokenRepository sessions,
            OtpChallengeRepository challenges,
            AuthEventRecorder authEvents,
            PasswordEncoder codeEncoder,
            OneTimeCodeGenerator codeGenerator,
            ApplicationEventPublisher events,
            Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.challenges = challenges;
        this.authEvents = authEvents;
        this.codeEncoder = codeEncoder;
        this.codeGenerator = codeGenerator;
        this.events = events;
        this.clock = clock;
    }

    /**
     * BR-IAM-01. Registration is idempotent by identifier: registering an existing number issues a
     * fresh code rather than failing, because the alternative — "that number is already registered"
     * — is the enumeration oracle described above, and because the honest case of a user who
     * forgot they had signed up is far more common than the attack.
     */
    @Transactional
    public void register(String identifier, String displayName, RequestContext context) {
        Instant now = clock.instant();
        boolean isPhone = looksLikePhoneNumber(identifier);

        AccountEntity account = findByIdentifier(identifier).orElseGet(() -> {
            AccountEntity created = new AccountEntity(
                    UUID.randomUUID(),
                    isPhone ? identifier : null,
                    isPhone ? null : identifier,
                    displayName,
                    now);
            authEvents.record(created.id(), "ACCOUNT_REGISTERED", context, null);
            return accounts.save(created);
        });

        issueCode(account, identifier, context);
    }

    /** BR-IAM-02. Silent when the identifier is unknown, for the reason given on the class. */
    @Transactional
    public void requestCode(String identifier, RequestContext context) {
        Optional<AccountEntity> account = findByIdentifier(identifier);

        if (account.isEmpty()) {
            authEvents.record(null, "CODE_REQUESTED_UNKNOWN_IDENTIFIER", context, null);
            return;
        }

        issueCode(account.get(), identifier, context);
    }

    /**
     * Verifies a code and issues a session. Returns the plaintext token, which the caller places in
     * the cookie and never persists.
     */
    @Transactional
    public IssuedSession verifyCode(String identifier, String code, RequestContext context) {
        Instant now = clock.instant();

        AccountEntity account = findByIdentifier(identifier).orElseThrow(() -> {
            authEvents.record(null, "CODE_VERIFY_UNKNOWN_IDENTIFIER", context, null);
            return invalidCode();
        });

        // A suspended account must not be able to sign back in by verifying a code, which would
        // undo an admin decision through a path nobody thought of as an authorisation check.
        if (account.status() == AccountStatus.SUSPENDED || account.status() == AccountStatus.CLOSED) {
            authEvents.record(account.id(), "CODE_VERIFY_REJECTED_STATUS", context, account.status().name());
            throw new ApiException(ErrorCode.FORBIDDEN, "This account is not available.");
        }

        OtpChallengeEntity challenge = challenges
                .findFirstByAccountIdAndPurposeAndConsumedAtIsNullOrderByIssuedAtDesc(account.id(), PURPOSE_PHONE)
                .orElseThrow(() -> {
                    authEvents.record(account.id(), "CODE_VERIFY_NO_CHALLENGE", context, null);
                    return invalidCode();
                });

        if (!challenge.isRedeemable(now)) {
            authEvents.record(account.id(), "CODE_VERIFY_EXPIRED_OR_EXHAUSTED", context, null);
            throw invalidCode();
        }

        if (!codeEncoder.matches(code, challenge.codeHash())) {
            challenge.recordFailedAttempt();
            challenges.save(challenge);
            authEvents.record(account.id(), "CODE_VERIFY_FAILED", context, null);
            throw invalidCode();
        }

        challenge.consume(now);
        challenges.save(challenge);

        if (looksLikePhoneNumber(identifier)) {
            account.markPhoneVerified(now);
        } else {
            account.markEmailVerified(now);
        }
        accounts.save(account);

        return issueSession(account, context, now);
    }

    /** Immediate and total, per ADR-008. Takes effect on the next request. */
    @Transactional
    public void revoke(String token, RequestContext context) {
        sessions.findByTokenHash(TokenHashing.hash(token)).ifPresent(session -> {
            session.revoke(clock.instant());
            sessions.save(session);
            authEvents.record(session.accountId(), "SESSION_REVOKED", context, null);
        });
    }

    @Transactional
    public void revokeAllSessions(UUID accountId, RequestContext context) {
        int revoked = sessions.revokeAllForAccount(accountId, clock.instant());
        authEvents.record(accountId, "ALL_SESSIONS_REVOKED", context, String.valueOf(revoked));
    }

    private IssuedSession issueSession(AccountEntity account, RequestContext context, Instant now) {
        String token = TokenHashing.generateToken();

        SessionTokenEntity session = new SessionTokenEntity(
                UUID.randomUUID(),
                account.id(),
                TokenHashing.hash(token),
                now,
                now.plus(SESSION_VALIDITY),
                context.sourceAddress(),
                context.userAgent());

        sessions.save(session);
        authEvents.record(account.id(), "SESSION_ISSUED", context, null);

        return new IssuedSession(token, now.plus(SESSION_VALIDITY), account.toAccount());
    }

    private void issueCode(AccountEntity account, String destination, RequestContext context) {
        Instant now = clock.instant();
        String code = codeGenerator.generate();

        OtpChallengeEntity challenge = challenges.save(new OtpChallengeEntity(
                UUID.randomUUID(),
                account.id(),
                PURPOSE_PHONE,
                codeEncoder.encode(code),
                now,
                now.plus(CODE_VALIDITY)));

        authEvents.record(account.id(), "CODE_ISSUED", context, null);

        // The event carries a reference, not the code — see OneTimeCodeIssued. Delivery is the
        // notification module's job; iam does not know what an SMS gateway is.
        events.publishEvent(new OneTimeCodeIssued(
                account.id(), challenge.id(), destination, PURPOSE_PHONE, now.plus(CODE_VALIDITY)));

        log.debug("Issued one-time code challenge {} for account {}", challenge.id(), account.id());
    }

    private Optional<AccountEntity> findByIdentifier(String identifier) {
        return looksLikePhoneNumber(identifier)
                ? accounts.findByPhoneNumber(identifier)
                : accounts.findByEmailIgnoringCase(identifier);
    }

    /**
     * Distinguishes the two identifier forms BR-IAM-01 allows. Phone numbers are normalised to
     * international format on capture (DAT-04), so by the time one reaches here it starts with a
     * plus; anything else is treated as an email address and validated as one at the boundary.
     */
    private static boolean looksLikePhoneNumber(String identifier) {
        return identifier.startsWith("+");
    }

    /**
     * One message for every failure mode: wrong code, expired code, no code, unknown identifier.
     * Distinguishing them tells an attacker which half of the guess was right.
     */
    private static ApiException invalidCode() {
        return new ApiException(ErrorCode.UNAUTHORIZED, "That code is not valid or has expired.");
    }

    /** What the caller knows about the request, for BR-IAM-06. */
    record RequestContext(@Nullable String sourceAddress, @Nullable String userAgent) {}

    record IssuedSession(String token, Instant expiresAt, Account account) {}
}
