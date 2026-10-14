package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.Account;
import com.jamlapps.meridian.iam.AccountStatus;
import com.jamlapps.meridian.iam.ImpersonationRecord;
import com.jamlapps.meridian.web.ApiException;
import com.jamlapps.meridian.web.AuditLog;
import com.jamlapps.meridian.web.ErrorCode;
import com.jamlapps.meridian.web.MessageGateway;
import com.jamlapps.meridian.web.RateLimiter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p><strong>Codes are never logged, persisted, or carried on an event.</strong> The plaintext code
 * exists only in {@link #issueCode}'s own stack, from generation to the synchronous {@code
 * web.MessageGateway.sendOneTimeCode} call that hands it to {@code notification} — see that
 * interface's javadoc for why this is the one place in this codebase a module calls another
 * synchronously to send something, rather than publishing an event.
 */
@Service
class AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationService.class);

    private static final Duration CODE_VALIDITY = Duration.ofMinutes(10);
    private static final Duration SESSION_VALIDITY = Duration.ofDays(30);
    private static final String PURPOSE_PHONE = "PHONE_VERIFICATION";

    /** BR-ADM-08: deliberately far shorter than {@link #SESSION_VALIDITY} — a support session, not a login. */
    private static final Duration IMPERSONATION_SESSION_VALIDITY = Duration.ofMinutes(30);

    private final AccountRepository accounts;
    private final SessionTokenRepository sessions;
    private final OtpChallengeRepository challenges;
    private final AuthEventRecorder authEvents;
    private final PasswordEncoder codeEncoder;
    private final OneTimeCodeGenerator codeGenerator;
    private final MessageGateway messageGateway;
    private final RateLimiter rateLimiter;
    private final AuditLog auditLog;
    private final Clock clock;

    AuthenticationService(
            AccountRepository accounts,
            SessionTokenRepository sessions,
            OtpChallengeRepository challenges,
            AuthEventRecorder authEvents,
            PasswordEncoder codeEncoder,
            OneTimeCodeGenerator codeGenerator,
            MessageGateway messageGateway,
            RateLimiter rateLimiter,
            AuditLog auditLog,
            Clock clock) {
        this.accounts = accounts;
        this.sessions = sessions;
        this.challenges = challenges;
        this.authEvents = authEvents;
        this.codeEncoder = codeEncoder;
        this.codeGenerator = codeGenerator;
        this.messageGateway = messageGateway;
        this.rateLimiter = rateLimiter;
        this.auditLog = auditLog;
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
        enforceOtcRateLimit(identifier, context);

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
        enforceOtcRateLimit(identifier, context);

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
        enforceAuthVerifyRateLimit(identifier, context);

        Instant now = clock.instant();

        AccountEntity account = findByIdentifier(identifier).orElseThrow(() -> {
            authEvents.record(null, "CODE_VERIFY_UNKNOWN_IDENTIFIER", context, null);
            return invalidCode();
        });

        // A suspended account must not be able to sign back in by verifying a code, which would
        // undo an admin decision through a path nobody thought of as an authorization check.
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

    /**
     * BR-ADM-08: mints a real, short-lived session for {@code targetAccountId}, distinguishable
     * from an ordinary login by {@code impersonatorAccountId} on the row itself. Three trails, not
     * one: {@code authEvents} (this class's own, like every other session-issuing path here),
     * {@code auditLog} (BR-ADM-07, an admin reviewing "who did this and why"), and the session row
     * itself, which is what {@link #impersonationHistory} reads back for BR-ADM-08's "visible to
     * the vendor" half. See {@code iam.ImpersonationSessions}'s javadoc for what this deliberately
     * does not do.
     */
    @Transactional
    public IssuedSession issueImpersonationSession(UUID targetAccountId, UUID impersonatorAccountId, String reason) {
        AccountEntity target = accounts.findById(targetAccountId).orElseThrow(() -> ApiException.notFound("Account", targetAccountId));

        String token = TokenHashing.generateToken();
        Instant now = clock.instant();
        Instant expiresAt = now.plus(IMPERSONATION_SESSION_VALIDITY);
        RequestContext context = new RequestContext(null, null);

        SessionTokenEntity session = new SessionTokenEntity(
                UUID.randomUUID(), target.id(), TokenHashing.hash(token), now, expiresAt, null, null, impersonatorAccountId);
        sessions.save(session);

        authEvents.record(target.id(), "IMPERSONATION_SESSION_ISSUED", context, "impersonatorAccountId=" + impersonatorAccountId);
        auditLog.record(impersonatorAccountId, "VENDOR_IMPERSONATION_STARTED", "Account", target.id().toString(), reason);

        return new IssuedSession(token, expiresAt, target.toAccount());
    }

    /** BR-ADM-08: {@code targetAccountId}'s own impersonation history, most recent first, without the token itself — never stored past issuance. */
    @Transactional(readOnly = true)
    public List<ImpersonationRecord> impersonationHistory(UUID targetAccountId) {
        return sessions.findByAccountIdAndImpersonatorAccountIdIsNotNullOrderByIssuedAtDesc(targetAccountId).stream()
                .map(session -> new ImpersonationRecord(
                        session.id(), session.impersonatorAccountId(), session.issuedAt(), session.expiresAt(), session.revokedAt()))
                .toList();
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
        Instant expiresAt = now.plus(CODE_VALIDITY);

        challenges.save(new OtpChallengeEntity(UUID.randomUUID(), account.id(), PURPOSE_PHONE, codeEncoder.encode(code), now, expiresAt));

        authEvents.record(account.id(), "CODE_ISSUED", context, null);

        // Synchronous, deliberately not an event — see web.MessageGateway's javadoc for why: the
        // plaintext code exists only in this method's own stack, from generation above to this
        // call, and must never be persisted or evented on its way anywhere else.
        messageGateway.sendOneTimeCode(destination, code, expiresAt);

        log.debug("Delivered one-time code for account {}", account.id());
    }

    /**
     * BR-SEC-02, AC-SEC-01: checked before {@link #findByIdentifier} in both {@link #register} and
     * {@link #requestCode} — an identifier that does not exist must be refused the same way and at
     * the same point as one that does, or the rate limit's own refusal becomes a second enumeration
     * oracle alongside the one this class's own doc already guards against.
     *
     * <p>Three checks, not two: {@code SCOPE_OTC_ISSUANCE_INTERVAL} is BR-SEC-02's "minimum
     * interval" half, by phone number only — a source address has no bearing on how soon the same
     * number may ask again, and checking it there would fail every second caller behind a shared
     * one. {@code SCOPE_OTC_ISSUANCE_VOLUME_IDENTIFIER}/{@code _SOURCE} are the "throttle" half,
     * BR-SEC-01's "by account and by source address" applied to a pre-account flow as phone number
     * and source address instead — see {@code web.RateLimiter}'s Javadoc for why the source rule's
     * default threshold is far more permissive than the identifier one.
     */
    private void enforceOtcRateLimit(String identifier, RequestContext context) {
        boolean intervalOk = rateLimiter.tryConsume(RateLimiter.SCOPE_OTC_ISSUANCE_INTERVAL, identifier);
        boolean volumeOk = rateLimiter.tryConsume(RateLimiter.SCOPE_OTC_ISSUANCE_VOLUME_IDENTIFIER, identifier);
        boolean sourceOk = context.sourceAddress() == null
                || rateLimiter.tryConsume(RateLimiter.SCOPE_OTC_ISSUANCE_VOLUME_SOURCE, context.sourceAddress());

        if (!intervalOk || !volumeOk || !sourceOk) {
            authEvents.record(null, "CODE_REQUEST_RATE_LIMITED", context, null);
            throw new ApiException(ErrorCode.RATE_LIMITED, "Too many code requests. Try again later.");
        }
    }

    /** BR-SEC-01: "authentication attempts", by identifier and by source address — same reasoning as {@link #enforceOtcRateLimit}. */
    private void enforceAuthVerifyRateLimit(String identifier, RequestContext context) {
        boolean identifierOk = rateLimiter.tryConsume(RateLimiter.SCOPE_AUTH_VERIFY_IDENTIFIER, identifier);
        boolean sourceOk = context.sourceAddress() == null
                || rateLimiter.tryConsume(RateLimiter.SCOPE_AUTH_VERIFY_SOURCE, context.sourceAddress());

        if (!identifierOk || !sourceOk) {
            authEvents.record(null, "CODE_VERIFY_RATE_LIMITED", context, null);
            throw new ApiException(ErrorCode.RATE_LIMITED, "Too many attempts. Try again later.");
        }
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
