package com.jamlapps.meridian.payment.internal;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * BR-ADM-03: transitions a stalled payment to {@code TIMED_OUT} without anything driving it from
 * outside — structured exactly like {@code tax.internal.TaxRetrySweeper}, calling through the
 * injected, proxied {@link PaymentService} rather than looping from inside that class.
 */
@Component
class PaymentTimeoutSweeper {

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutSweeper.class);

    private final PaymentService payments;

    PaymentTimeoutSweeper(PaymentService payments) {
        this.payments = payments;
    }

    @Scheduled(fixedDelay = 60_000)
    void sweep() {
        List<UUID> candidates = payments.timeoutCandidateIds();
        for (UUID id : candidates) {
            payments.tryTimeout(id);
        }
        if (!candidates.isEmpty()) {
            log.info("Payment timeout sweep transitioned {} transaction(s)", candidates.size());
        }
    }
}
