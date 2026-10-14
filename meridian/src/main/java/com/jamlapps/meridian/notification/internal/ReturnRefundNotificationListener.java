package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.returns.events.ReturnRefunded;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * Not itself an enumerated BR-NTF-01/02 case — those name order confirmation/dispatch/delivery and
 * new-order/settlement-completion specifically — but a natural extension of the same "tell the
 * vendor when money moves against their account" pattern {@link SettlementNotificationListener}
 * and {@link OrderCancellationNotificationListener} already cover, using an event ADR-002 already
 * lists among this module's allowed dependencies ({@code returns::events}).
 */
@Component
class ReturnRefundNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(ReturnRefundNotificationListener.class);

    private final ContactResolver contacts;
    private final NotificationService notifications;

    ReturnRefundNotificationListener(ContactResolver contacts, NotificationService notifications) {
        this.contacts = contacts;
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(ReturnRefunded event) {
        contacts.forVendor(event.vendorId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "A refund of " + event.refundAmount() + " was issued for sub-order " + event.subOrderId() + ".",
                                "Refund issued",
                                "Return " + event.returnId() + " for sub-order " + event.subOrderId() + " (order " + event.parentOrderId()
                                        + ") was refunded " + event.refundAmount() + " at " + event.refundedAt() + "."),
                        () -> log.warn("No contact on file for vendor {}; refund notification not sent", event.vendorId()));
    }
}
