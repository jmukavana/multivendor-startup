package com.jamlapps.meridian.notification.internal;

/** ADR-006, DEP-07: the SMS aggregator, behind an interface in this module's own vocabulary. */
interface SmsGateway {

    GatewayTransmission send(String phoneNumber, String body);

    record GatewayTransmission(boolean accepted, String providerReference) {}
}
