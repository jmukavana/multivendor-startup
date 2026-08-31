package com.jamlapps.meridian.iam.internal;

import com.jamlapps.meridian.iam.internal.AuthenticationService.RequestContext;
import java.time.Clock;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/** BR-IAM-06: writes the authentication audit trail. */
@Component
class AuthEventRecorder {

    private final AuthEventRepository repository;
    private final Clock clock;

    AuthEventRecorder(AuthEventRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    void record(@Nullable UUID accountId, String eventType, RequestContext context, @Nullable String detail) {
        repository.save(new AuthEventEntity(
                UUID.randomUUID(),
                accountId,
                eventType,
                clock.instant(),
                context.sourceAddress(),
                context.userAgent(),
                detail));
    }
}
