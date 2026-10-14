package com.jamlapps.meridian.tax.internal;

import java.security.SecureRandom;

/**
 * REG-04: a provisional invoice numbering scheme, matching {@code order.internal
 * .OrderReferenceGenerator}'s exact reasoning — collisions on an 8-character reference are
 * effectively impossible, so this does not retry on one. DigiTax onboarding (DEP-04) may dictate
 * a real numbering convention (eTIMS typically expects one tied to the taxpayer's own sequence);
 * replacing this is a single-file change when that happens.
 */
final class TaxInvoiceNumberGenerator {

    private static final String PREFIX = "INV-";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I, easy to read aloud
    private static final int LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TaxInvoiceNumberGenerator() {}

    static String generate() {
        StringBuilder builder = new StringBuilder(PREFIX.length() + LENGTH);
        builder.append(PREFIX);
        for (int i = 0; i < LENGTH; i++) {
            builder.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
