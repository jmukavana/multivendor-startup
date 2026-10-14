package com.jamlapps.meridian.fulfilment;

import java.util.UUID;

/** BR-FUL-01/03. */
public record Carrier(UUID id, String name, boolean active) {}
