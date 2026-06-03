package com.iispl.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility class for password hashing.
 * Uses SHA-256 (one-way hash). Production should ideally use BCrypt with salt,
 * but SHA-256 is sufficient for trainee learning and project demo.
 */
public class PasswordUtil {

    /**
     * Hash a plain text password using SHA-256.
     *
     * @param plainPassword The raw password entered by user
     * @return Hex string of SHA-256 hash (64 characters)
     */
    public static String hash(String plainPassword) {
        if (plainPassword == null) return null;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Password hashing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Convenience for verification — same as hash().equals(stored).
     */
    public static boolean matches(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null) return false;
        return hash(plainPassword).equals(storedHash);
    }
}