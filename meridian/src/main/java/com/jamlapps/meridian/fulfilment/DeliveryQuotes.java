package com.jamlapps.meridian.fulfilment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * BR-CHK-06/07: how {@code checkout} prices delivery before payment. Read-only, matching {@code
 * iam.Accounts}' reasoning — quoting never mutates anything; a quote becomes real only once {@code
 * DeliverySelections.select} records it against a sub-order.
 */
public interface DeliveryQuotes {

    /** BR-CHK-05: the zones a buyer may choose as a destination. */
    List<DeliveryZone> activeZones();

    /**
     * BR-CHK-06: the cheapest current rate card among active carriers for this zone and method.
     * Empty when no active carrier has ever been given a rate card for this exact combination —
     * a real "delivery is not available here" answer, not an error.
     */
    Optional<DeliveryQuote> cheapestQuote(UUID zoneId, DeliveryMethod method);
}
