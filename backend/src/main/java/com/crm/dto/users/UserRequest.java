package com.crm.dto.users;

public class UserRequest {
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private Long teamId;

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Long getTeamId() { return teamId; }
}
