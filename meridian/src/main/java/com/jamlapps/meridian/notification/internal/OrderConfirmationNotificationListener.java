package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.order.events.OrderPaymentConfirmed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-NTF-01: order confirmation to the buyer, and BR-NTF-02: new-order notification to each
 * vendor with a sub-order in it. One event, two audiences — {@code
 * OrderPaymentConfirmed.subOrders} already carries the {@code vendorId} for each, so this is one
 * listener rather than splitting the vendor half onto its own class.
 */
@Component
class OrderConfirmationNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationNotificationListener.class);

    private final ContactResolver contacts;
    private final NotificationService notifications;

    OrderConfirmationNotificationListener(ContactResolver contacts, NotificationService notifications) {
        this.contacts = contacts;
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(OrderPaymentConfirmed event) {
        contacts.forAccount(event.buyerAccountId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "Your Meridian order " + event.reference() + " is confirmed.",
                                "Order confirmed",
                                "Your order " + event.reference() + " was confirmed at " + event.confirmedAt() + "."),
                        () -> log.warn("No contact on file for buyer {}; order {} confirmation not sent", event.buyerAccountId(), event.reference()));

        event.subOrders().forEach(subOrder -> contacts.forVendor(subOrder.vendorId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "You have a new order: sub-order " + subOrder.subOrderId() + " from order " + event.reference() + ".",
                                "New order received",
                                "Sub-order " + subOrder.subOrderId() + " from order " + event.reference() + " (" + subOrder.subtotal() + ")."),
                        () -> log.warn("No contact on file for vendor {}; new-order notification not sent", subOrder.vendorId())));
    }
}
