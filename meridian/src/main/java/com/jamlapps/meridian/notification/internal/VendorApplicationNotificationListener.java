package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.vendor.events.VendorApplicationApproved;
import com.jamlapps.meridian.vendor.events.VendorApplicationStatusChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-VND-06, AC-VND-01: notifies the applicant of every state change, including the 60-second SLA
 * on approval.
 *
 * <p>{@code @ApplicationModuleListener} runs synchronously with the publishing transaction's
 * commit and is durable via the event publication registry (ADR-004), so "within 60 seconds" is
 * satisfied by this running immediately after approval, not by any polling or scheduling here.
 */
@Component
class VendorApplicationNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(VendorApplicationNotificationListener.class);

    private final ContactResolver contacts;
    private final NotificationService notifications;

    VendorApplicationNotificationListener(ContactResolver contacts, NotificationService notifications) {
        this.contacts = contacts;
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(VendorApplicationApproved event) {
        notify(
                event.accountId(),
                "Congratulations — " + event.businessName() + " is now live on Meridian.",
                "Vendor application approved",
                "Your vendor application " + event.applicationId() + " for " + event.businessName() + " was approved; vendor "
                        + event.vendorId() + " is now live.");
    }

    @ApplicationModuleListener
    void on(VendorApplicationStatusChanged event) {
        String reasonSuffix = event.reason() == null ? "" : " (" + event.reason() + ")";
        notify(
                event.accountId(),
                "Your vendor application moved to " + event.status() + reasonSuffix + ".",
                "Vendor application update",
                "Vendor application " + event.applicationId() + " moved to " + event.status() + reasonSuffix + " at " + event.occurredAt() + ".");
    }

    private void notify(java.util.UUID accountId, String smsBody, String emailSubject, String emailBody) {
        contacts.forAccount(accountId)
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(contact.phoneNumber(), contact.email(), smsBody, emailSubject, emailBody),
                        () -> log.warn("No contact on file for account {}; vendor application notification not sent", accountId));
    }
}
