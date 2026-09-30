package com.crm.dao.permissions;

import com.crm.model.Role;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO layer managing User - Role many-to-many relationship in user_roles table (CRM-29).
 * Supports multi-role assignment and role retrieval via JDBC.
 */
public class UserRoleDAO {
    private static final Logger LOGGER = Logger.getLogger(UserRoleDAO.class.getName());

    /**
     * Find all role IDs assigned to a user.
     */
    public List<Long> findRoleIdsByUserId(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return List.of();
        }

        String sql = "SELECT role_id FROM user_roles WHERE user_id = ? ORDER BY role_id";
        List<Long> roleIds = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roleIds.add(rs.getLong("role_id"));
                }
            }
        }
        return roleIds;
    }

    public List<Long> findRoleIdsByUserId(long userId) {
        if (userId <= 0) {
            return List.of();
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findRoleIdsByUserId(conn, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding role IDs for userId: " + userId, e);
            return List.of();
        }
    }

    /**
     * Find all Role entities assigned to a user.
     */
    public List<Role> findRolesByUserId(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return List.of();
        }

        String sql = "SELECT r.id, r.name "
                + "FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ? "
                + "ORDER BY r.name";

        List<Role> roles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    roles.add(new Role(rs.getLong("id"), rs.getString("name")));
                }
            }
        }
        return roles;
    }

    public List<Role> findRolesByUserId(long userId) {
        if (userId <= 0) {
            return List.of();
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findRolesByUserId(conn, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding roles for userId: " + userId, e);
            return List.of();
        }
    }

    /**
     * Replace all roles of a user with a new collection of role IDs (Multi-role support).
     */
    public void replaceUserRoles(Connection conn, long userId, Collection<Long> roleIds) throws SQLException {
        if (conn == null || userId <= 0) {
            return;
        }

        String deleteSql = "DELETE FROM user_roles WHERE user_id = ?";
        try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
            deleteStmt.setLong(1, userId);
            deleteStmt.executeUpdate();
        }

        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        // Deduplicate and insert
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>(roleIds);
        String insertSql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            for (Long roleId : uniqueIds) {
                if (roleId != null && roleId > 0) {
                    insertStmt.setLong(1, userId);
                    insertStmt.setLong(2, roleId);
                    insertStmt.addBatch();
                }
            }
            insertStmt.executeBatch();
        }
    }

    /**
     * Add a single role to a user.
     */
    public void addRole(Connection conn, long userId, long roleId) throws SQLException {
        if (conn == null || userId <= 0 || roleId <= 0) {
            return;
        }
        String sql = "INSERT IGNORE INTO user_roles (user_id, role_id) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, roleId);
            stmt.executeUpdate();
        }
    }

    /**
     * Remove a single role from a user.
     */
    public void removeRole(Connection conn, long userId, long roleId) throws SQLException {
        if (conn == null || userId <= 0 || roleId <= 0) {
            return;
        }
        String sql = "DELETE FROM user_roles WHERE user_id = ? AND role_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, roleId);
            stmt.executeUpdate();
        }
    }

    /**
     * Remove all roles from a user.
     */
    public void removeAllRoles(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return;
        }
        String sql = "DELETE FROM user_roles WHERE user_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Retrieve all available roles defined in the system.
     */
    public List<Role> findAllRoles(Connection conn) throws SQLException {
        if (conn == null) {
            return List.of();
        }

        String sql = "SELECT id, name FROM roles ORDER BY id";
        List<Role> roles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                roles.add(new Role(rs.getLong("id"), rs.getString("name")));
            }
        }
        return roles;
    }

    public List<Role> findAllRoles() {
        try (Connection conn = DBConnection.getConnection()) {
            return findAllRoles(conn);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all roles", e);
            return List.of();
        }
    }

    /**
     * Find role ID by role name (case-insensitive).
     */
    public Long findRoleIdByName(Connection conn, String roleName) throws SQLException {
        if (conn == null || roleName == null || roleName.isBlank()) {
            return null;
        }

        String sql = "SELECT id FROM roles WHERE LOWER(TRIM(name)) = LOWER(TRIM(?)) LIMIT 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
            }
        }
        return null;
    }

    /**
     * Check if user currently holds a role with the given name.
     */
    public boolean hasRole(Connection conn, long userId, String roleName) throws SQLException {
        if (conn == null || userId <= 0 || roleName == null || roleName.isBlank()) {
            return false;
        }

        String sql = "SELECT 1 FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ? AND LOWER(TRIM(r.name)) = LOWER(TRIM(?)) "
                + "LIMIT 1";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, roleName);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Check if a set of role IDs contains a role with the given name.
     */
    public boolean containsRoleName(Connection conn, Collection<Long> roleIds, String roleName) throws SQLException {
        if (roleIds == null || roleIds.isEmpty() || roleName == null || roleName.isBlank()) {
            return false;
        }
        Long targetRoleId = findRoleIdByName(conn, roleName);
        return targetRoleId != null && roleIds.contains(targetRoleId);
    }
}
