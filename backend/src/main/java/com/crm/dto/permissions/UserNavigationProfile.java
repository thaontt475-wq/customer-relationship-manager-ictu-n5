package com.crm.dto.permissions;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * User profile details required for role-based navigation sidebar & header display (CRM-26 AC 2).
 * Cung cấp thông tin: Tên hiển thị, Vai trò và Nhóm kinh doanh đang thuộc về.
 */
public class UserNavigationProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long userId;
    private final String displayName;
    private final String fullName;
    private final String role;
    private final List<String> roles;
    private final String teamName;

    public UserNavigationProfile(long userId, String displayName, String fullName, String role, String teamName) {
        this(userId, displayName, fullName, role, teamName, null);
    }

    public UserNavigationProfile(long userId, String displayName, String fullName, String role, String teamName, Collection<String> roles) {
        this.userId = userId;
        this.fullName = fullName != null ? fullName.trim() : "";
        if (displayName != null && !displayName.isBlank()) {
            this.displayName = displayName.trim();
        } else if (!this.fullName.isBlank()) {
            this.displayName = this.fullName;
        } else {
            this.displayName = "Người dùng";
        }

        this.role = (role != null && !role.isBlank()) ? role.trim() : "Người dùng";
        this.teamName = (teamName != null && !teamName.isBlank()) ? teamName.trim() : "Chưa phân nhóm";

        if (roles != null && !roles.isEmpty()) {
            this.roles = List.copyOf(new ArrayList<>(roles));
        } else if (role != null && !role.isBlank()) {
            List<String> parsed = new ArrayList<>();
            for (String r : role.split(",")) {
                if (r != null && !r.isBlank()) {
                    parsed.add(r.trim());
                }
            }
            this.roles = List.copyOf(parsed);
        } else {
            this.roles = List.of();
        }
    }

    public long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public List<String> getRoles() {
        return roles;
    }

    public String getTeamName() {
        return teamName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserNavigationProfile that = (UserNavigationProfile) o;
        return userId == that.userId &&
                Objects.equals(displayName, that.displayName) &&
                Objects.equals(role, that.role) &&
                Objects.equals(teamName, that.teamName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, displayName, role, teamName);
    }

    @Override
    public String toString() {
        return "UserNavigationProfile{" +
                "userId=" + userId +
                ", displayName='" + displayName + '\'' +
                ", role='" + role + '\'' +
                ", teamName='" + teamName + '\'' +
                '}';
    }
}
