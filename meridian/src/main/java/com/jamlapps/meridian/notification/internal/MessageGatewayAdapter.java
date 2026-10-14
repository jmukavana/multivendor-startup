package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.notification.NotificationChannel;
import com.jamlapps.meridian.web.MessageGateway;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * BR-IAM-01/02: the real implementation behind {@code web.MessageGateway} — see that interface's
 * javadoc for why this is a synchronous slot rather than this module's usual
 * {@code @ApplicationModuleListener} reaction. Delivery mechanics — channel choice, the persisted
 * {@code NotificationMessageEntity} row, admin-queue visibility — are identical to every other
 * transactional send this module makes, via the same {@link NotificationService#sendTransactional};
 * only the caller and the synchronous timing differ.
 */
@Component
class MessageGatewayAdapter implements MessageGateway {

    private final NotificationService notifications;

    MessageGatewayAdapter(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override
    public void sendOneTimeCode(String destination, String code, Instant expiresAt) {
        String body = "Your Meridian verification code is " + code + ". It expires at " + expiresAt + ".";
        NotificationChannel channel = looksLikeEmail(destination) ? NotificationChannel.EMAIL : NotificationChannel.SMS;
        notifications.sendTransactional(channel, destination, "Meridian verification code", body);
    }

    /** Same distinguishing idiom {@code iam.internal.AuthenticationService.looksLikePhoneNumber} uses, kept local for the same reason the code it replaces was. */
    private static boolean looksLikeEmail(String destination) {
        return destination.indexOf('@') >= 0;
    }
}
