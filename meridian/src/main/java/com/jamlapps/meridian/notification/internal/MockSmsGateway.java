package com.jamlapps.meridian.notification.internal;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real SMS aggregator (DEP-07, ADR-006) — unbranded, unlike {@code
 * tax.internal.MockDigiTaxGateway}, because no specific aggregator has been named for this
 * integration; a Kenyan bulk-SMS provider (Africa's Talking is the common choice) is the likely
 * real replacement once DEP-07 closes, but naming this after one now would be a guess this
 * codebase's other mocks have avoided making without an explicit steer.
 *
 * <p>Always succeeds immediately, the same limitation every other mock gateway in this codebase
 * carries — there is no failure path to exercise BR-NTF-05's retry against until a real gateway or
 * a deliberately-failing test double replaces this.
 */
@Component
class MockSmsGateway implements SmsGateway {

    private static final Logger log = LoggerFactory.getLogger(MockSmsGateway.class);

    @Override
    public GatewayTransmission send(String phoneNumber, String body) {
        String reference = "MOCK-SMS-" + UUID.randomUUID();
        log.info("[DEV ONLY — no SMS gateway configured] SMS to {}: \"{}\" ({})", phoneNumber, body, reference);
        return new GatewayTransmission(true, reference);
    }
}
