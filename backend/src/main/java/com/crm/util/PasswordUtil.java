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
     * - at least one lower, one upper, one digit, one special character
     */
    public static boolean isValidPassword(String password) {
        if (password == null) return false;
        if (password.length() < 8 || password.length() > 72) return false;
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[^a-zA-Z0-9].*");
        return hasLower && hasUpper && hasDigit && hasSpecial;
    }
}
