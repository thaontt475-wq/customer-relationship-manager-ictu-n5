package com.crm.service.users;

public class AvatarException extends Exception {
    private final int status;
    public AvatarException(int status, String message) {
        super(message);
        this.status = status;
    }
    public int getStatus() { return status; }
}
