package com.jamlapps.meridian.inventory;

import java.util.UUID;

/**
 * BR-INV-04, AC-INV-01: thrown when a reservation would exceed what is available. A checked-style
 * signal rather than {@code web.ApiException} directly — {@code inventory} does not depend on
 * {@code web}'s error vocabulary for its own domain outcome; the caller (checkout) decides how to
 * present it.
 */
public class InsufficientStockException extends RuntimeException {

    private final UUID productId;
    private final int requested;
    private final int available;

    public InsufficientStockException(UUID productId, int requested, int available) {
        super("Only " + available + " of product " + productId + " are available; " + requested + " requested.");
        this.productId = productId;
        this.requested = requested;
        this.available = available;
    }

    public UUID productId() {
        return productId;
    }

    public int requested() {
        return requested;
    }

    public int available() {
        return available;
    }
}
