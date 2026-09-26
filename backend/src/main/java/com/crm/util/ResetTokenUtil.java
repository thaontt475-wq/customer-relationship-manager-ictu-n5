package com.crm.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;

public class ResetTokenUtil {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTE_LENGTH = 32; // 256 bits
    private static final long EXPIRY_MINUTES = 15L;

    /**
     * Generates a raw token string (URL‑safe Base64, no padding).
     */
    public static String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Returns the SHA‑256 hash of the raw token as a lowercase hex string (64 chars).
     */
    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes());
            StringBuilder sb = new StringBuilder(2 * hashed.length);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA‑256 is guaranteed to exist on a Java platform
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Calculates the expiry time (now + 15 minutes) as a {@link LocalDateTime} in the system default zone.
     */
    public static LocalDateTime calculateExpiry() {
        Instant now = Instant.now();
        return LocalDateTime.ofInstant(now.plusSeconds(EXPIRY_MINUTES * 60), ZoneId.systemDefault());
    }
}
