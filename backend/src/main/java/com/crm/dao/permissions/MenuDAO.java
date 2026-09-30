package com.crm.dao.permissions;

import com.crm.dto.permissions.UserNavigationProfile;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO layer executing database queries via JDBC API for CRM-26 menu navigation.
 * Architecture: Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0.
 */
public class MenuDAO {
    private static final Logger LOGGER = Logger.getLogger(MenuDAO.class.getName());

    /**
     * Query all role names assigned to a given user ID via active connection.
     *
     * @param conn   active database connection
     * @param userId target user ID
     * @return list of role names
     * @throws SQLException on database errors
     */
    public List<String> findRoleNamesByUserId(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return List.of();
        }

        String sql = "SELECT r.name "
                + "FROM user_roles ur "
                + "JOIN roles r ON r.id = ur.role_id "
                + "WHERE ur.user_id = ? "
                + "ORDER BY r.name";

        List<String> roles = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("name");
                    if (name != null && !name.isBlank()) {
                        roles.add(name.trim());
                    }
                }
            }
        }
        return roles;
    }

    /**
     * Query role names assigned to a given user ID, managing connection lifecycle.
     *
     * @param userId target user ID
     * @return list of role names
     */
    public List<String> findRoleNamesByUserId(long userId) {
        if (userId <= 0) {
            return List.of();
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findRoleNamesByUserId(conn, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying user roles for userId: " + userId, e);
            return List.of();
        }
    }

    /**
     * Query user navigation profile details (Name, Roles, Team) for CRM-26 AC 2.
     *
     * @param conn   active database connection
     * @param userId target user ID
     * @return UserNavigationProfile with name, roles, team name
     * @throws SQLException on database errors
     */
    public UserNavigationProfile findUserNavigationProfile(Connection conn, long userId) throws SQLException {
        if (conn == null || userId <= 0) {
            return null;
        }

        String sql = "SELECT u.id, u.username, u.email, u.full_name, u.display_name, u.status, u.team_id, "
                + "t.name AS team_name, "
                + "(SELECT GROUP_CONCAT(r.name ORDER BY r.name SEPARATOR ', ') "
                + " FROM user_roles ur JOIN roles r ON r.id = ur.role_id "
                + " WHERE ur.user_id = u.id) AS role_names "
                + "FROM users u "
                + "LEFT JOIN teams t ON t.id = u.team_id "
                + "WHERE u.id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    long id = rs.getLong("id");
                    String fullName = rs.getString("full_name");
                    String displayName = rs.getString("display_name");
                    if (displayName == null || displayName.isBlank()) {
                        displayName = (fullName != null && !fullName.isBlank()) ? fullName : rs.getString("username");
                    }
                    String rolesStr = rs.getString("role_names");
                    String teamName = rs.getString("team_name");
                    return new UserNavigationProfile(id, displayName, fullName, rolesStr, teamName);
                }
            }
        }
        return null;
    }

    /**
     * Query user navigation profile details managing connection lifecycle.
     *
     * @param userId target user ID
     * @return UserNavigationProfile or null if not found
     */
    public UserNavigationProfile findUserNavigationProfile(long userId) {
        if (userId <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return findUserNavigationProfile(conn, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying user navigation profile for userId: " + userId, e);
            return null;
        }
    }

    /**
     * Check if a user possesses a specific role in the database.
     *
     * @param conn     active database connection
     * @param userId   target user ID
     * @param roleName role name to check
     * @return true if user has role
     * @throws SQLException on database errors
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
     * Check if a user possesses a specific role, managing connection lifecycle.
     *
     * @param userId   target user ID
     * @param roleName role name to check
     * @return true if user has role
     */
    public boolean hasRole(long userId, String roleName) {
        if (userId <= 0 || roleName == null || roleName.isBlank()) {
            return false;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return hasRole(conn, userId, roleName);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error checking user role for userId: " + userId + ", role: " + roleName, e);
            return false;
        }
    }
}
