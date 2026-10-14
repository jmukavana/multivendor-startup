package com.jamlapps.meridian.notification.internal;

import com.jamlapps.meridian.order.events.SubOrderCancelled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * BR-ORD-08/16: "notify the vendor immediately on cancellation" — {@code event.reason()}
 * distinguishes BR-ORD-13's buyer-initiated case from BR-ORD-05's auto-cancellation (the vendor's
 * own window lapsing), so the vendor reads which one actually happened rather than a message that
 * assumes it was always the buyer.
 *
 * <p>BR-ORD-16's other half — "shall not count a buyer-initiated cancellation against the vendor's
 * cancellation rate under BR-VND-13" — needs no code here: BR-VND-13 (a vendor cancellation-rate
 * metric) is not built anywhere in {@code vendor} yet, so there is no rate for this cancellation to
 * be wrongly counted against. Recorded as a pre-existing gap this listener does not introduce, not
 * a requirement satisfied by omission — and, once BR-VND-13 exists, {@code reason} is exactly what
 * would let it exclude the buyer-initiated case while still counting the vendor's own lapse.
 */
@Component
class OrderCancellationNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(OrderCancellationNotificationListener.class);

    private final ContactResolver contacts;
    private final NotificationService notifications;

    OrderCancellationNotificationListener(ContactResolver contacts, NotificationService notifications) {
        this.contacts = contacts;
        this.notifications = notifications;
    }

    @ApplicationModuleListener
    void on(SubOrderCancelled event) {
        contacts.forVendor(event.vendorId())
                .ifPresentOrElse(
                        contact -> notifications.notifyContact(
                                contact.phoneNumber(),
                                contact.email(),
                                "Sub-order " + event.subOrderId() + " was cancelled: " + event.reason() + ".",
                                "Order cancelled",
                                "Sub-order " + event.subOrderId() + " (from order " + event.parentOrderId() + ") was cancelled at "
                                        + event.cancelledAt() + ": " + event.reason() + "."),
                        () -> log.warn("No contact on file for vendor {}; cancellation notification not sent", event.vendorId()));
    }
}
