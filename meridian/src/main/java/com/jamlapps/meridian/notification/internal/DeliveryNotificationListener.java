package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.fulfilment.events.DeliveryCompleted;
import com.jamlapps.meridian.fulfilment.events.DeliveryDispatched;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/** BR-NTF-01: the buyer is told on dispatch and on delivery, the two halves order confirmation alone did not cover. */
@Component
class DeliveryNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(DeliveryNotificationListener.class);

    private final ContactResolver contacts;
    private final NotificationService notifications;

    DeliveryNotificationListener(ContactResolver contacts, NotificationService notifications) {
        this.contacts = contacts;
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(DeliveryDispatched event) {
        contacts.forAccount(event.buyerAccountId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "Your order is on its way — tracking reference " + event.trackingReference() + ".",
                                "Order dispatched",
                                "Sub-order " + event.subOrderId() + " (from order " + event.parentOrderId() + ") was dispatched at "
                                        + event.dispatchedAt() + ", tracking reference " + event.trackingReference() + "."),
                        () -> log.warn("No contact on file for buyer {}; dispatch notification not sent", event.buyerAccountId()));
    }

    @ApplicationModuleListener
    void on(DeliveryCompleted event) {
        contacts.forAccount(event.buyerAccountId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "Your order has been delivered.",
                                "Order delivered",
                                "Sub-order " + event.subOrderId() + " (from order " + event.parentOrderId() + ") was delivered at "
                                        + event.deliveredAt() + "."),
                        () -> log.warn("No contact on file for buyer {}; delivery notification not sent", event.buyerAccountId()));
    }
}
