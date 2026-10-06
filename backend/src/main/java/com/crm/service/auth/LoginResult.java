package com.crm.service.auth;

import com.crm.model.users.User;

import java.time.LocalDateTime;

public class LoginResult {

    public enum Status {
        SUCCESS,
        INVALID,
        LOCKED
    }

    private Status status;
    private User user;
    private Integer remainingAttempts;
    private LocalDateTime lockedUntil;

    private LoginResult() {
    }

    public static LoginResult success(User user) {

        LoginResult result =
                new LoginResult();

        result.status = Status.SUCCESS;
        result.user = user;

        return result;
    }

    public static LoginResult invalid(
            Integer remainingAttempts
    ) {

        LoginResult result =
                new LoginResult();

        result.status = Status.INVALID;
        result.remainingAttempts =
                remainingAttempts;

        return result;
    }

    public static LoginResult locked(
            LocalDateTime lockedUntil
    ) {

        LoginResult result =
                new LoginResult();

        result.status = Status.LOCKED;
        result.lockedUntil =
                lockedUntil;

        return result;
    }

    public Status getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    public Integer getRemainingAttempts() {
        return remainingAttempts;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}