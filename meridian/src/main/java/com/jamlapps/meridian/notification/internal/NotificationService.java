package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.notification.NotificationChannel;
import com.jamlapps.meridian.notification.NotificationMessage;
import com.jamlapps.meridian.notification.NotificationStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BR-NTF-01 to 05. Deliberately works in phone numbers, emails and message bodies only — resolving
 * an account or vendor id to a destination is {@code ContactResolver}'s job, not this class's; see
 * this module's package doc on why that split keeps the {@code iam}/{@code vendor} dependency
 * confined to one place. Structured like {@code tax.internal.TaxService}: no atomic-claim pattern,
 * because a duplicate SMS is not the kind of harm BR-STL-12's exactly-once disbursement guards
 * against, and self-invocation from {@code send*} to {@code transmit} is safe for the same reason
 * — neither method uses an atomic-claim {@code @Modifying} query.
 */
@Service
class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMessageRepository messages;
    private final NotificationPreferenceRepository preferences;
    private final SmsGateway smsGateway;
    private final EmailGateway emailGateway;
    private final Clock clock;

    NotificationService(
            NotificationMessageRepository messages,
            NotificationPreferenceRepository preferences,
            SmsGateway smsGateway,
            EmailGateway emailGateway,
            Clock clock) {
        this.messages = messages;
        this.preferences = preferences;
        this.smsGateway = smsGateway;
        this.emailGateway = emailGateway;
        this.clock = clock;
    }

    /** BR-NTF-01/02: enqueues and attempts one transactional message. Never gated by the BR-NTF-04 opt-out. */
    @Transactional
    void sendTransactional(NotificationChannel channel, String recipient, @Nullable String subject, String body) {
        NotificationMessageEntity message =
                messages.save(new NotificationMessageEntity(UUID.randomUUID(), channel, recipient, subject, body, clock.instant()));
        transmit(message.id());
    }

    /**
     * BR-NTF-01/03: a convenience over {@link #sendTransactional} for the common case — SMS to a
     * phone number when one is on file, email as the BR-NTF-03 secondary channel when one is on
     * file too. Either or both may be absent (an account can be phone-only or email-only); neither
     * being present is not an error here, only nothing to deliver — {@code ContactResolver}'s
     * caller decides whether a missing contact is itself worth logging.
     */
    @Transactional
    void notifyContact(@Nullable String phoneNumber, @Nullable String email, String smsBody, String emailSubject, String emailBody) {
        if (phoneNumber != null) {
            sendTransactional(NotificationChannel.SMS, phoneNumber, null, smsBody);
        }
        if (email != null) {
            sendTransactional(NotificationChannel.EMAIL, email, emailSubject, emailBody);
        }
    }

    /**
     * BR-NTF-04: the one send path this module has that the opt-out gates. Infrastructure ahead of
     * use — see this module's package doc on why nothing calls this yet.
     */
    @Transactional
    void sendMarketing(UUID accountId, NotificationChannel channel, String recipient, @Nullable String subject, String body) {
        boolean optedOut = preferences.findByAccountId(accountId).map(NotificationPreferenceEntity::marketingOptOut).orElse(false);
        if (optedOut) {
            log.debug("Marketing send to account {} suppressed (opted out)", accountId);
            return;
        }
        sendTransactional(channel, recipient, subject, body);
    }

    /**
     * BR-NTF-05: attempts delivery once. Called both from {@code send*} (the first attempt) and
     * {@link NotificationRetrySweeper} (every retry) — a plain method, not looped from inside this
     * class, matching {@code tax.internal.TaxService.transmitInvoice}'s reasoning exactly.
     */
    @Transactional
    void transmit(UUID messageId) {
        NotificationMessageEntity message = messages.findById(messageId).orElseThrow();

        if (message.status() != NotificationStatus.PENDING && message.status() != NotificationStatus.FAILED) {
            return;
        }

        try {
            boolean accepted = switch (message.channel()) {
                case SMS -> smsGateway.send(message.recipient(), message.body()).accepted();
                case EMAIL -> emailGateway.send(message.recipient(), null, message.body()).accepted();
            };
            if (accepted) {
                message.recordSuccess(clock.instant());
            } else {
                message.recordFailure();
            }
        } catch (RuntimeException e) {
            log.warn("Notification delivery failed for {} {}: {}", message.channel(), message.recipient(), e.getMessage());
            message.recordFailure();
        }

        messages.save(message);
    }

    @Transactional(readOnly = true)
    List<UUID> retryableMessageIds() {
        return messages.findByStatus(NotificationStatus.FAILED).stream().map(NotificationMessageEntity::id).toList();
    }

    /** BR-NTF-05: FAILED (still retrying) and DEAD_LETTERED (exhausted) both belong in the admin exception queue. */
    @Transactional(readOnly = true)
    List<NotificationMessage> exceptionMessages() {
        return java.util.stream.Stream.concat(
                        messages.findByStatus(NotificationStatus.FAILED).stream(),
                        messages.findByStatus(NotificationStatus.DEAD_LETTERED).stream())
                .map(NotificationMessageEntity::toNotificationMessage)
                .toList();
    }

    /** Ops/test visibility: every message ever queued for one recipient, most recent first. */
    @Transactional(readOnly = true)
    List<NotificationMessage> forRecipient(String recipient) {
        return messages.findByRecipientOrderByCreatedAtDesc(recipient).stream().map(NotificationMessageEntity::toNotificationMessage).toList();
    }

    /** BR-NTF-04: the caller's own preference. */
    @Transactional(readOnly = true)
    boolean marketingOptOut(UUID accountId) {
        return preferences.findByAccountId(accountId).map(NotificationPreferenceEntity::marketingOptOut).orElse(false);
    }

    /** BR-NTF-04: self-service opt-out/opt-in. */
    @Transactional
    void setMarketingOptOut(UUID accountId, boolean optOut) {
        Instant now = clock.instant();
        NotificationPreferenceEntity preference = preferences
                .findByAccountId(accountId)
                .orElseGet(() -> new NotificationPreferenceEntity(accountId, false, now));
        preference.update(optOut, now);
        preferences.save(preference);
    }
}
