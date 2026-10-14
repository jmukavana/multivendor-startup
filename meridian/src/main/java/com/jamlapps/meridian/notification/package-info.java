/**
 * Notifications.
 *
 * <p>Implements BR-NTF of the business requirements document. Delivered in phase 2 onward,
 * completed in phase 9.
 *
 * <p><strong>{@code iam} and {@code vendor} are supersets of ADR-002's table</strong> — it lists
 * only {@code events of iam, order, payment, settlement, returns, vendor}. Every event this module
 * listens to names an account or vendor by id, never by phone number or email — {@code
 * SubOrderCancelled} carries {@code buyerAccountId}, {@code VendorDisbursed} carries {@code
 * vendorId}, and so on. An event-only dependency can log that id; it cannot address an SMS
 * gateway. Real delivery needs {@code iam.Accounts} to resolve an account id to a phone number or
 * email, and {@code vendor.Vendors} to resolve a vendor id to its {@code ownerAccountId} first.
 * Both are read-only query facades, the same shape {@code returns} and {@code review} already
 * added {@code vendor} for, and neither introduces a cycle: {@code iam} depends on nothing, and
 * {@code vendor} depends only on {@code iam}, so neither depends back on {@code notification}.
 * {@code notification.internal.ContactResolver} is the one class in this module that uses either —
 * every other class here works in phone numbers, emails and message bodies, not account or vendor
 * ids.
 *
 * <p><strong>{@code iam::events} was removed, closing a real bug this module's own test coverage
 * never caught.</strong> One-time codes used to be delivered by {@code
 * OneTimeCodeNotificationListener} reacting to {@code iam.events.OneTimeCodeIssued} — an event that
 * deliberately never carried the plaintext code (a credential at rest in the event publication
 * registry is a real security concern), only a {@code deliveryReference}. Reasonable in isolation,
 * except nothing ever resolved that reference back to a code — it cannot be: {@code iam} only ever
 * stores a BCrypt hash. The listener sent a real SMS with a real reference number and no code in
 * it. Every automated test used {@code CapturingOneTimeCodeGenerator}, a test-only double that
 * captures the plaintext code before {@code iam} hashes and discards it, which exercises
 * verification correctly but never exercises delivery at all — the gap was invisible to every
 * {@code @SpringBootTest} in this codebase and was found only by running the real application and
 * reading its own mock-SMS log line. {@code web.MessageGateway} (filled by {@code
 * MessageGatewayAdapter}, this module's own {@code NotificationService.sendTransactional} under
 * it) replaces the whole event path: {@code iam} now delivers the code synchronously, in the same
 * method that generated it, before it is ever discarded. See {@code web.MessageGateway}'s javadoc
 * and ADR-004's "Note added" section for why a synchronous call is the deliberate exception here,
 * not a lapse back into direct coupling.
 *
 * <p><strong>{@code fulfilment::events} is a third superset entry, added when BR-NTF-01's
 * "dispatch" and "delivery" SMS were built.</strong> This was previously deferred here on the
 * belief that resolving the buyer to notify would need {@code order} on top of {@code
 * fulfilment::events} — wrong: {@code fulfilment.internal.DeliveryEntity} already carries {@code
 * buyerAccountId} (captured off {@code order.events.OrderPaymentConfirmed} when the delivery row
 * is created), it simply was not on the public events yet. {@code DeliveryDispatched} was added
 * carrying it from the start, and {@code DeliveryCompleted} gained the field — both zero-ripple
 * changes, since neither event had a consumer before this. {@code
 * notification.internal.DeliveryNotificationListener} resolves the buyer's contact from {@code
 * buyerAccountId} exactly the way every other listener in this module already does; no further
 * dependency was needed. BR-NTF-01 is now fully real: order confirmation, dispatch and delivery
 * all send.
 *
 * <p><strong>BR-NTF-04's opt-out is infrastructure ahead of use.</strong> {@code
 * NotificationService.sendMarketing} checks {@code notification_preference} before enqueueing and
 * {@code NotificationPreferenceController} lets a buyer set it, but nothing in this codebase yet
 * generates a marketing send for it to gate — there is no marketing-campaign feature anywhere in
 * the built modules. Every listener in this module calls {@code sendTransactional}, which never
 * consults the preference, matching BR-NTF-04's "opt-out retains transactional notifications"
 * exactly. The gate exists and is real; it has no caller yet to exercise it, which is recorded
 * here rather than left to look accidental.
 *
 * <p>Types in this package form the module's public API. Everything in a sub-package is internal
 * and inaccessible to other modules, enforced by {@code ModularityTests}.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Notifications",
        allowedDependencies = {
            "iam",
            "vendor",
            "vendor::events",
            "order::events",
            "payment::events",
            "settlement::events",
            "returns::events",
            "fulfilment::events"
        })
package com.jamlapps.meridian.notification;
