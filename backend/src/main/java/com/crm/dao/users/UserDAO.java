package com.crm.dao.users;

import com.crm.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {
    public User findForLogin(String email) throws SQLException {
        String sql = "SELECT u.id, u.email, u.password_hash, "
                + "COALESCE(u.display_name, u.full_name) AS display_name, u.active, u.status, "
                + "r.id AS role_id, r.name AS role_name FROM users u "
                + "LEFT JOIN user_roles ur ON ur.user_id = u.id "
                + "LEFT JOIN roles r ON r.id = ur.role_id WHERE u.email = ? ORDER BY r.id";
        try (Connection conn = com.crm.util.DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                User user = null;
                List<com.crm.model.Role> roles = new ArrayList<>();
                while (rs.next()) {
                    if (user == null) {
                        user = new User();
                        user.setId(rs.getLong("id"));
                        user.setEmail(rs.getString("email"));
                        user.setPasswordHash(rs.getString("password_hash"));
                        user.setDisplayName(rs.getString("display_name"));
                        user.setActive(rs.getBoolean("active"));
                        user.setStatus(rs.getString("status"));
                    }
                    long roleId = rs.getLong("role_id");
                    if (!rs.wasNull()) roles.add(new com.crm.model.Role(roleId, rs.getString("role_name")));
                }
                if (user != null) user.setRoles(roles);
                return user;
            }
        }
    }


    public User findByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, full_name, phone, status FROM users WHERE email = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User();
                    user.setId(rs.getLong("id"));
                    user.setUsername(rs.getString("username"));
                    user.setEmail(rs.getString("email"));
                    user.setPasswordHash(rs.getString("password_hash"));
                    user.setFullName(rs.getString("full_name"));
                    user.setPhone(rs.getString("phone"));
                    user.setStatus(rs.getString("status"));
                    return user;
                }
            }
        }
        return null;
    }

    public void updatePasswordHash(Connection conn, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setLong(2, userId);
            stmt.executeUpdate();
        }
    }

    public List<User> findAll(Connection conn) throws SQLException {
        String sql = "SELECT id, username, email, full_name, phone, status, "
                + "created_at, updated_at, last_login_at FROM users ORDER BY full_name, email";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapViewUser(rs));
            }
        }
        return users;
    }

    public User findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT id, username, email, full_name, phone, status, "
                + "created_at, updated_at, last_login_at FROM users WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapViewUser(rs) : null;
            }
        }
    }

    public User findByIdForUpdate(Connection conn, long id) throws SQLException {
        String sql = "SELECT id, username, email, full_name, phone, status, "
                + "created_at, updated_at, last_login_at FROM users WHERE id = ? FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapViewUser(rs) : null;
            }
        }
    }

    public List<User> findActiveRecipientsExcluding(Connection conn, long excludedUserId) throws SQLException {
        String sql = "SELECT id, username, email, full_name, phone, status, "
                + "created_at, updated_at, last_login_at FROM users "
                + "WHERE status = 'ACTIVE' AND id <> ? ORDER BY full_name, email";
        List<User> users = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, excludedUserId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapViewUser(rs));
                }
            }
        }
        return users;
    }

    public int updateStatus(Connection conn, long userId, String expectedStatus, String newStatus)
            throws SQLException {
        String sql = "UPDATE users SET status = ? WHERE id = ? AND status = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setLong(2, userId);
            stmt.setString(3, expectedStatus);
            return stmt.executeUpdate();
        }
    }

    private User mapViewUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setPhone(rs.getString("phone"));
        user.setStatus(rs.getString("status"));
        user.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        user.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        user.setLastLoginAt(toLocalDateTime(rs.getTimestamp("last_login_at")));
        return user;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }
}
