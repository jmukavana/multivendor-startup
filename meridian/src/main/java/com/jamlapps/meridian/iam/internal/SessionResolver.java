package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.AccountStatus;
import com.jamlapps.meridian.iam.MeridianPrincipal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves a bearer token into a principal, touching the session's last-used timestamp along the
 * way.
 *
 * <p>Split out from {@code SessionAuthenticationFilter} deliberately: {@code @Transactional} does
 * not belong on a class extending {@code OncePerRequestFilter}. Spring AOP proxies a transactional
 * method with CGLIB, and that proxy is instantiated through Objenesis, which bypasses every
 * constructor in the class hierarchy — including {@code GenericFilterBean}'s own field
 * initializers. The result is a filter whose {@code logger} field is silently {@code null}, which
 * surfaces as an NPE the first time the filter chain runs, in code this class never touches. A
 * plain {@code @Service} has no such constraint.
 */
@Service
class SessionResolver {

    private final SessionTokenRepository sessions;
    private final AccountRepository accounts;
    private final Clock clock;

    SessionResolver(SessionTokenRepository sessions, AccountRepository accounts, Clock clock) {
        this.sessions = sessions;
        this.accounts = accounts;
        this.clock = clock;
    }

    @Transactional
    Optional<MeridianPrincipal> resolve(String token) {
        var now = clock.instant();

        return sessions.findByTokenHash(TokenHashing.hash(token))
                .filter(session -> session.isLive(now))
                .flatMap(session -> accounts.findById(session.accountId()).map(account -> {
                    session.touch(now);
                    return account;
                }))
                // A suspended account's existing sessions are revoked on suspension, but this is
                // the second gate: a session that outlives that revocation for any reason still
                // cannot authenticate. BR-VND-10 and BR-SEC-07 both depend on this being immediate.
                .filter(account -> account.status() == AccountStatus.ACTIVE)
                .map(account -> new MeridianPrincipal(account.id(), account.toAccount()));
    }

    static List<SimpleGrantedAuthority> authoritiesOf(MeridianPrincipal principal) {
        return principal.account().roles().stream()
                .map(role -> new SimpleGrantedAuthority(role.authority()))
                .toList();
    }
}
