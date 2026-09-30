package com.crm.dao.excel;

import com.crm.dto.excel.ImportRowData;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DAO layer managing database batch checks and persistence for Excel User Import (CRM-32).
 */
public class UserImportDAO {
    private static final Logger LOGGER = Logger.getLogger(UserImportDAO.class.getName());

    /**
     * Check which emails among the provided set already exist in the database.
     */
    public Set<String> findExistingEmails(Connection conn, Collection<String> emails) throws SQLException {
        Set<String> existing = new HashSet<>();
        if (conn == null || emails == null || emails.isEmpty()) {
            return existing;
        }

        StringBuilder sql = new StringBuilder("SELECT LOWER(email) AS email FROM users WHERE LOWER(email) IN (");
        int index = 0;
        for (String ignored : emails) {
            if (index > 0) sql.append(",");
            sql.append("?");
            index++;
        }
        sql.append(")");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (String email : emails) {
                stmt.setString(paramIndex++, email != null ? email.trim().toLowerCase(Locale.ROOT) : "");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getString("email"));
                }
            }
        }
        return existing;
    }

    /**
     * Check which usernames among the provided set already exist in the database.
     */
    public Set<String> findExistingUsernames(Connection conn, Collection<String> usernames) throws SQLException {
        Set<String> existing = new HashSet<>();
        if (conn == null || usernames == null || usernames.isEmpty()) {
            return existing;
        }

        StringBuilder sql = new StringBuilder("SELECT LOWER(username) AS username FROM users WHERE LOWER(username) IN (");
        int index = 0;
        for (String ignored : usernames) {
            if (index > 0) sql.append(",");
            sql.append("?");
            index++;
        }
        sql.append(")");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (String username : usernames) {
                stmt.setString(paramIndex++, username != null ? username.trim().toLowerCase(Locale.ROOT) : "");
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    existing.add(rs.getString("username"));
                }
            }
        }
        return existing;
    }

    /**
     * Retrieve all roles as a Map of normalized name -> Role ID.
     */
    public Map<String, Long> findAllRolesMap(Connection conn) throws SQLException {
        Map<String, Long> roleMap = new HashMap<>();
        if (conn == null) {
            return roleMap;
        }

        String sql = "SELECT id, name FROM roles";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    roleMap.put(name.trim().toLowerCase(Locale.ROOT), rs.getLong("id"));
                }
            }
        }
        return roleMap;
    }

    /**
     * Retrieve all teams as a Map of normalized name -> Team ID.
     */
    public Map<String, Long> findAllTeamsMap(Connection conn) throws SQLException {
        Map<String, Long> teamMap = new HashMap<>();
        if (conn == null) {
            return teamMap;
        }

        String sql = "SELECT id, name FROM teams";
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                if (name != null) {
                    teamMap.put(name.trim().toLowerCase(Locale.ROOT), rs.getLong("id"));
                }
            }
        }
        return teamMap;
    }

    /**
     * Persist valid imported users into the database inside a transaction.
     * Inserts into `users` and assigns roles into `user_roles`.
     *
     * @param conn                active JDBC connection
     * @param validRows           list of valid rows to insert
     * @param defaultPasswordHash pre-hashed default password
     * @return number of users successfully created
     */
    public int saveImportedUsers(Connection conn, List<ImportRowData> validRows, String defaultPasswordHash)
            throws SQLException {
        if (conn == null || validRows == null || validRows.isEmpty()) {
            return 0;
        }

        String insertUserSql = "INSERT INTO users "
                + "(username, email, password_hash, full_name, display_name, active, phone, status, team_id, data_scope) "
                + "VALUES (?, ?, ?, ?, ?, TRUE, ?, 'ACTIVE', ?, 'SELF')";

        String insertRoleSql = "INSERT INTO user_roles (user_id, role_id) VALUES (?, ?)";

        int createdCount = 0;

        try (PreparedStatement userStmt = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement roleStmt = conn.prepareStatement(insertRoleSql)) {

            for (ImportRowData row : validRows) {
                userStmt.setString(1, row.getUsername());
                userStmt.setString(2, row.getEmail());
                userStmt.setString(3, defaultPasswordHash);
                userStmt.setString(4, row.getFullName());
                userStmt.setString(5, row.getFullName());
                userStmt.setString(6, row.getPhone() != null && !row.getPhone().isBlank() ? row.getPhone() : null);

                if (row.getResolvedTeamId() != null && row.getResolvedTeamId() > 0) {
                    userStmt.setLong(7, row.getResolvedTeamId());
                } else {
                    userStmt.setNull(7, Types.BIGINT);
                }

                userStmt.executeUpdate();

                try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        long userId = generatedKeys.getLong(1);
                        row.setCreatedUserId(userId);
                        createdCount++;

                        // Assign role
                        if (row.getResolvedRoleId() != null && row.getResolvedRoleId() > 0) {
                            roleStmt.setLong(1, userId);
                            roleStmt.setLong(2, row.getResolvedRoleId());
                            roleStmt.executeUpdate();
                        }
                    }
                }
            }
        }

        return createdCount;
    }
}
