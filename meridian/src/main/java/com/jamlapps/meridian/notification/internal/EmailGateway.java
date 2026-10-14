package com.jamlapps.meridian.notification.internal;

import org.jspecify.annotations.Nullable;

/** ADR-006: BR-NTF-03's secondary channel, behind an interface in this module's own vocabulary. */
interface EmailGateway {

    GatewayTransmission send(String emailAddress, @Nullable String subject, String body);

    record GatewayTransmission(boolean accepted, String providerReference) {}
}
