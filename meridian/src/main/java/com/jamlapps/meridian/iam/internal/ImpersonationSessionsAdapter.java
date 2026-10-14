package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.ImpersonationRecord;
import com.jamlapps.meridian.iam.ImpersonationSessions;
import com.jamlapps.meridian.iam.IssuedImpersonationSession;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ImpersonationSessionsAdapter implements ImpersonationSessions {

    private final AuthenticationService authentication;

    ImpersonationSessionsAdapter(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    @Override
    public IssuedImpersonationSession issue(UUID targetAccountId, UUID impersonatorAccountId, String reason) {
        var issued = authentication.issueImpersonationSession(targetAccountId, impersonatorAccountId, reason);
        return new IssuedImpersonationSession(issued.token(), issued.expiresAt(), targetAccountId, impersonatorAccountId);
    }

    @Override
    public List<ImpersonationRecord> historyFor(UUID targetAccountId) {
        return authentication.impersonationHistory(targetAccountId);
    }
}
