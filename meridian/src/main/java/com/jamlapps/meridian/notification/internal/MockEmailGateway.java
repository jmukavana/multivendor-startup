package com.jamlapps.meridian.notification.internal;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real transactional email provider (DEP-07, ADR-006) — same unbranded reasoning
 * as {@link MockSmsGateway}, and the same "always succeeds" limitation.
 */
@Component
class MockEmailGateway implements EmailGateway {

    private static final Logger log = LoggerFactory.getLogger(MockEmailGateway.class);

    @Override
    public GatewayTransmission send(String emailAddress, @Nullable String subject, String body) {
        String reference = "MOCK-EMAIL-" + UUID.randomUUID();
        log.info(
                "[DEV ONLY — no email gateway configured] Email to {} ({}): \"{}\" ({})", emailAddress, subject, body, reference);
        return new GatewayTransmission(true, reference);
    }
}
