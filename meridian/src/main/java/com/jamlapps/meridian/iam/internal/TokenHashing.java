package com.jamlapps.meridian.iam.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generation and hashing of session tokens, per ADR-008.
 *
 * <p>SHA-256 rather than a password KDF is deliberate. The token carries 256 bits of entropy from
 * {@link SecureRandom}, so there is nothing to brute-force, and it must be looked up by hash on
 * every authenticated request — a deliberately slow hash would add latency to every call for no
 * security gain. One-time codes are the opposite case and use BCrypt.
 */
final class TokenHashing {

    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private TokenHashing() {}

    /** A new token in the form the client receives it. Never stored. */
    static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException cause) {
            // SHA-256 is required of every JVM. If it is absent the platform is broken in a way no
            // fallback would make safe.
            throw new IllegalStateException("SHA-256 is unavailable", cause);
        }
    }

    /** A six-digit code, zero-padded so every code is the same length. */
    static String generateOneTimeCode() {
        return "%06d".formatted(RANDOM.nextInt(1_000_000));
    }
}
