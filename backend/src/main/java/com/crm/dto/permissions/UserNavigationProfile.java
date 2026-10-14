package com.crm.dto.permissions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO chứa thông tin người dùng cho thanh điều hướng và menu (CRM-26 AC 2).
 * Bao gồm: Tên (Họ tên / Tên hiển thị), Vai trò (Roles) và Nhóm kinh doanh (Team).
 */
public class UserNavigationProfile {
    private final long userId;
    private final String fullName;
    private final String displayName;
    private final String role;
    private final List<String> roles;
    private final Long teamId;
    private final String teamName;

    public UserNavigationProfile(
            long userId,
            String fullName,
            String displayName,
            String role,
            List<String> roles,
            Long teamId,
            String teamName
    ) {
        this.userId = userId;
        this.fullName = fullName != null ? fullName : "";
        this.displayName = (displayName != null && !displayName.isBlank())
                ? displayName
                : (fullName != null && !fullName.isBlank() ? fullName : "Người dùng");
        this.role = (role != null && !role.isBlank()) ? role : "Người dùng";
        this.roles = roles != null ? Collections.unmodifiableList(new ArrayList<>(roles)) : List.of();
        this.teamId = teamId;
        this.teamName = (teamName != null && !teamName.isBlank()) ? teamName : "Chưa thuộc nhóm";
    }

    public long getUserId() {
        return userId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public List<String> getRoles() {
        return roles;
    }

    public Long getTeamId() {
        return teamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", userId);
        map.put("id", userId);
        map.put("fullName", fullName);
        map.put("displayName", displayName);
        map.put("role", role);
        map.put("roles", roles);
        map.put("teamId", teamId);
        map.put("teamName", teamName);
        return map;
    }
}
