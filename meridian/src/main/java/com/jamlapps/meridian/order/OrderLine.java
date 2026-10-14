package com.jamlapps.meridian.order;

import com.jamlapps.meridian.shared.Money;
import java.util.UUID;

public record OrderLine(UUID productId, String productNameSnapshot, Money unitPrice, int quantity, Money lineTotal) {}
