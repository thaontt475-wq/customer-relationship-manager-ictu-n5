package com.crm.dto.users;

public class UserWriteRequest {

    private String fullName;
    private String email;
    private String password;
    private String status;

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getStatus() {
        return status;
    }
}