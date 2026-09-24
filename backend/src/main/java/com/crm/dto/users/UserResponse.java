package com.crm.dto.users;

import com.crm.model.users.User;

public class UserResponse {

    private Long id;
    private String email;
    private String fullName;
    private String phone;
    private Long teamId;
    private boolean active;

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public Long getTeamId() {
        return teamId;
    }

    public boolean isActive() {
        return active;
    }

    public static UserResponse from(User user) {

        UserResponse response = new UserResponse();

        response.id = user.getId();
        response.email = user.getEmail();
        response.fullName = user.getFullName();
        response.phone = user.getPhone();
        response.teamId = user.getTeamId();
        response.active = user.isActive();

        return response;
    }
}