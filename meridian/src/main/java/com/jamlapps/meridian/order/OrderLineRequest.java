package com.jamlapps.meridian.order;

import java.util.UUID;

public record OrderLineRequest(UUID productId, int quantity) {}
