package com.jamlapps.meridian.order.internal;

import java.security.SecureRandom;

/**
 * BR-ORD-01: unique reference in the format {@code MER-XXXXXXXX}.
 *
 * <p>The {@code MER-} prefix is provisional — OPEN-09 records that "Meridian" carries no brand
 * intent and the order reference prefix is set pending naming. It lives here as a single
 * constant precisely so that when a name is chosen, changing it is one line and not a grep across
 * the codebase.
 */
final class OrderReferenceGenerator {

    private static final String PREFIX = "MER-";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I, easy to read aloud
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private OrderReferenceGenerator() {}

    static String generate() {
        StringBuilder builder = new StringBuilder(PREFIX.length() + LENGTH);
        builder.append(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
