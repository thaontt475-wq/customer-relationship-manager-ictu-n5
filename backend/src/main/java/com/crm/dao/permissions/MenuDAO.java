package com.crm.dao.permissions;

import com.crm.config.DatabaseConfig;
import com.crm.dto.permissions.UserNavigationProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO layer thực hiện truy vấn cơ sở dữ liệu qua JDBC API cho tính năng điều hướng Menu (CRM-26).
 * Kiến trúc chuẩn: Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0.
 */
public class MenuDAO {
    private static final Logger LOGGER = Logger.getLogger(MenuDAO.class.getName());

    /**
     * Lấy thông tin điều hướng của người dùng: Tên, Vai trò và Nhóm kinh doanh (AC 2).
     */
    public UserNavigationProfile findUserNavigationProfile(long userId) {
        if (userId <= 0) {
            return null;
        }

        String userSql = """
                SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.status,
                       u.team_id, t.name AS team_name
                FROM users u
                LEFT JOIN teams t ON t.id = u.team_id
                WHERE u.id = ?
                """;

        String roleSql = """
                SELECT r.id, r.code, r.name
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.name
                """;

        try (Connection conn = DatabaseConfig.getConnection()) {
            String fullName = null;
            String displayName = null;
            Long teamId = null;
            String teamName = null;

            try (PreparedStatement stmt = conn.prepareStatement(userSql)) {
                stmt.setLong(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        fullName = rs.getString("full_name");
                        displayName = rs.getString("display_name");
                        if (displayName == null || displayName.isBlank()) {
                            displayName = (fullName != null && !fullName.isBlank())
                                    ? fullName
                                    : rs.getString("username");
                        }
                        long tid = rs.getLong("team_id");
                        if (!rs.wasNull()) {
                            teamId = tid;
                        }
                        teamName = rs.getString("team_name");
                    } else {
                        return null;
                    }
                }
            }

            List<String> roleNames = new ArrayList<>();
            try (PreparedStatement roleStmt = conn.prepareStatement(roleSql)) {
                roleStmt.setLong(1, userId);
                try (ResultSet rs = roleStmt.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString("name");
                        if (name == null || name.isBlank()) {
                            name = rs.getString("code");
                        }
                        if (name != null && !name.isBlank()) {
                            roleNames.add(name.trim());
                        }
                    }
                }
            }

            String primaryRole = roleNames.isEmpty() ? "Người dùng" : String.join(", ", roleNames);
            return new UserNavigationProfile(
                    userId,
                    fullName,
                    displayName,
                    primaryRole,
                    roleNames,
                    teamId,
                    teamName
            );

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy vấn thông tin điều hướng của người dùng với ID: " + userId, e);
            return null;
        }
    }

    /**
     * Lấy danh sách mã quyền (permissions) của người dùng từ cơ sở dữ liệu qua JDBC.
     */
    public Set<String> findPermissionsByUserId(long userId) {
        if (userId <= 0) {
            return Set.of();
        }

        String sql = """
                SELECT DISTINCT p.code
                FROM permissions p
                JOIN role_permissions rp ON rp.permission_id = p.id
                JOIN user_roles ur ON ur.role_id = rp.role_id
                WHERE ur.user_id = ?
                """;

        Set<String> permissions = new HashSet<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    if (code != null && !code.isBlank()) {
                        permissions.add(code.trim());
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy vấn permissions của userId: " + userId, e);
        }
        return permissions;
    }

    /**
     * Lấy danh sách mã vai trò (role codes) của người dùng.
     */
    public List<String> findRoleCodesByUserId(long userId) {
        if (userId <= 0) {
            return List.of();
        }

        String sql = """
                SELECT r.code
                FROM roles r
                JOIN user_roles ur ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.code
                """;

        List<String> roles = new ArrayList<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String code = rs.getString("code");
                    if (code != null && !code.isBlank()) {
                        roles.add(code.trim().toUpperCase());
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi truy vấn roles của userId: " + userId, e);
        }
        return roles;
    }
}
