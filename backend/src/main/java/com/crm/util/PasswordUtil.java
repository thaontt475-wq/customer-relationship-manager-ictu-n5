package com.crm.util;

import org.mindrot.jbcrypt.BCrypt;

public final class PasswordUtil {

    private PasswordUtil() {
    }

    public static String hash(
            String password
    ) {
        return BCrypt.hashpw(
                password,
                BCrypt.gensalt(12)
        );
    }

    public static boolean matches(
            String password,
            String passwordHash
    ) {

        if (
                password == null ||
                passwordHash == null
        ) {
            return false;
        }

        try {
            return BCrypt.checkpw(
                    password,
                    passwordHash
            );
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}