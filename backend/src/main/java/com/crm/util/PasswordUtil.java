package com.crm.util;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {
    private static final int COST = 12;

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(COST));
    }

    public static boolean verifyPassword(String plain, String hash) {
        if (hash == null || hash.isEmpty()) return false;
        return BCrypt.checkpw(plain, hash);
    }

    /**
     * Validate password policy:
     * - 8 to 72 characters
     * - at least one letter
     * - at least one digit
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8 || password.length() > 72) return false;
        boolean hasLetter = password.matches(".*[A-Za-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        return hasLetter && hasDigit;
    }
}
