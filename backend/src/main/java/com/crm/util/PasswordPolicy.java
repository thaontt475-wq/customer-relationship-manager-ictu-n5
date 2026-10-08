package com.crm.util;

public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static boolean isValid(
            String password
    ) {

        if (
                password == null ||
                password.length() < 8
        ) {
            return false;
        }

        boolean hasLetter = false;
        boolean hasDigit = false;

        for (char c : password.toCharArray()) {

            if (Character.isLetter(c)) {
                hasLetter = true;
            }

            if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }

        return hasLetter && hasDigit;
    }
}