package com.jamlapps.meridian.fulfilment.events;

import java.time.Instant;
import java.util.UUID;

/**
 * BR-FUL-06/07, BR-NTF-01: published on delivery completion. Consumed by {@code
 * notification.internal.DeliveryNotificationListener} (BR-NTF-01's "notify on delivery") — but not
 * by {@code order}, to drive its own DELIVERED transition: see this module's package doc on why
 * that would close a module dependency cycle, and is not how that transition is triggered.
 *
 * @param buyerAccountId added for BR-NTF-01's dispatch/delivery notifications — {@code
 *     notification} resolves a real destination from it via {@code iam.Accounts}, the same
 *     contact-resolution path it already uses for order confirmation
 */
public record DeliveryCompleted(UUID subOrderId, UUID parentOrderId, UUID deliveryId, UUID buyerAccountId, Instant deliveredAt) {}
