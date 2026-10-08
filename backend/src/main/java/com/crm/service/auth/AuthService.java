package com.crm.service.auth;

import com.crm.dao.users.UserDAO;
import com.crm.model.users.User;
import com.crm.util.PasswordUtil;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class AuthService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserDAO userDAO =
            new UserDAO();

    public LoginResult login(
            String email,
            String password
    ) throws Exception {

        if (
                email == null ||
                email.isBlank() ||
                password == null ||
                password.isEmpty()
        ) {

            return LoginResult.invalid(null);
        }

        User user =
                userDAO.findByEmail(
                        email.trim().toLowerCase()
                );

        if (
                user == null ||
                !"ACTIVE".equalsIgnoreCase(
                        user.getStatus()
                )
        ) {

            return LoginResult.invalid(null);
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (
                user.getLockedUntil() != null &&
                user.getLockedUntil().isAfter(now)
        ) {

            return LoginResult.locked(
                    user.getLockedUntil()
            );
        }

        if (
                user.getLockedUntil() != null &&
                !user.getLockedUntil().isAfter(now)
        ) {

            userDAO.updateLoginState(
                    user.getId(),
                    0,
                    null
            );

            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        }

        if (
                !PasswordUtil.matches(
                        password,
                        user.getPasswordHash()
                )
        ) {

            int attempts =
                    user.getFailedLoginAttempts() + 1;

            if (attempts >= MAX_ATTEMPTS) {

                LocalDateTime lockedUntil =
                        now.plusMinutes(
                                LOCK_MINUTES
                        );

                userDAO.updateLoginState(
                        user.getId(),
                        attempts,
                        Timestamp.valueOf(
                                lockedUntil
                        )
                );

                return LoginResult.locked(
                        lockedUntil
                );
            }

            userDAO.updateLoginState(
                    user.getId(),
                    attempts,
                    null
            );

            return LoginResult.invalid(
                    MAX_ATTEMPTS - attempts
            );
        }

        userDAO.updateLoginState(
                user.getId(),
                0,
                null
        );

        return LoginResult.success(user);
    }
}