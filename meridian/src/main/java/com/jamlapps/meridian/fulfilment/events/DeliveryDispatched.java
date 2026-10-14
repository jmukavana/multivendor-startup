package com.jamlapps.meridian.fulfilment.events;

import java.time.Instant;
import java.util.UUID;

/**
 * BR-FUL-04, BR-NTF-01: published on dispatch, whether reached through {@code DeliveryService
 * .dispatch} (the carrier gateway) or {@code #dispatchManually} (a vendor's own hand-entered
 * tracking reference) — both are real dispatch, and BR-NTF-01's "notify on dispatch" does not care
 * which path got there.
 */
public record DeliveryDispatched(UUID subOrderId, UUID parentOrderId, UUID deliveryId, UUID buyerAccountId, String trackingReference, Instant dispatchedAt) {}
