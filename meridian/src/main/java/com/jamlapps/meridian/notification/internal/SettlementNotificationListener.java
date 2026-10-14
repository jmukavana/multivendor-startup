package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.settlement.events.VendorDisbursed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-NTF-02: "vendor notifications for ... settlement completion." */
@Component
class SettlementNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(SettlementNotificationListener.class);

    private final ContactResolver contacts;
    private final NotificationService notifications;

    SettlementNotificationListener(ContactResolver contacts, NotificationService notifications) {
        this.contacts = contacts;
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(VendorDisbursed event) {
        contacts.forVendor(event.vendorId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "Settlement disbursed: " + event.amount() + " (ref " + event.disbursementReference() + ").",
                                "Settlement disbursed",
                                "Settlement run " + event.settlementRunId() + " disbursed " + event.amount() + " to you at "
                                        + event.disbursedAt() + " (reference " + event.disbursementReference() + ")."),
                        () -> log.warn("No contact on file for vendor {}; settlement notification not sent", event.vendorId()));
    }
}
