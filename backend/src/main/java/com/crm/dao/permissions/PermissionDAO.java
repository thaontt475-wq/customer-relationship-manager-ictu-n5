package com.crm.dao.permissions;

import com.crm.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PermissionDAO {

    public List<Role> findAllRoles(Connection conn) throws SQLException {
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

    public List<Long> findRoleIdsByUserId(Connection conn, long userId) throws SQLException {
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

    public boolean allRolesExist(Connection conn, List<Long> roleIds) throws SQLException {
        if (roleIds == null || roleIds.isEmpty()) {
            return true;
        }

        String placeholders = String.join(",", Collections.nCopies(roleIds.size(), "?"));
        String sql = "SELECT COUNT(*) FROM roles WHERE id IN (" + placeholders + ")";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < roleIds.size(); i++) {
                stmt.setLong(i + 1, roleIds.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt(1) == roleIds.size();
            }
        }
    }

    public void replaceUserRoles(Connection conn, long userId, List<Long> roleIds) throws SQLException {
        try (PreparedStatement deleteStmt =
                     conn.prepareStatement("DELETE FROM user_roles WHERE user_id = ?")) {
            deleteStmt.setLong(1, userId);
            deleteStmt.executeUpdate();
        }

        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }

        try (PreparedStatement insertStmt =
                     conn.prepareStatement("INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)")) {
            for (Long roleId : roleIds) {
                insertStmt.setLong(1, userId);
                insertStmt.setLong(2, roleId);
                insertStmt.addBatch();
            }
            insertStmt.executeBatch();
        }
    }

    public int updateDataScope(Connection conn, long userId, String dataScope) throws SQLException {
        String sql = "UPDATE users SET data_scope = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, dataScope);
            stmt.setLong(2, userId);
            return stmt.executeUpdate();
        }
    }
}
