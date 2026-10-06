package com.crm.dao.auth;

import com.crm.config.DatabaseConfig;
import java.sql.*;
import java.util.*;

public class SessionDAO {
    public Map<String, Object> findAccount(long id) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                 SELECT u.id, u.email, u.full_name, u.status, u.session_version,
                        u.data_scope, u.team_id, t.name AS team_name
                 FROM users u LEFT JOIN teams t ON t.id = u.team_id WHERE u.id = ?
                 """)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) return null;
                Map<String, Object> account = new LinkedHashMap<>();
                account.put("id", rs.getLong("id"));
                account.put("userId", rs.getLong("id"));
                account.put("email", rs.getString("email"));
                account.put("fullName", rs.getString("full_name"));
                account.put("status", rs.getString("status"));
                account.put("sessionVersion", rs.getLong("session_version"));
                account.put("dataScope", rs.getString("data_scope"));
                account.put("teamId", rs.getObject("team_id"));
                account.put("teamName", rs.getString("team_name"));
                return account;
            }
        }
    }

    public boolean changePassword(long id, String currentPassword, String newHash) throws SQLException {
        try (Connection connection = DatabaseConfig.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement query = connection.prepareStatement(
                        "SELECT password_hash FROM users WHERE id = ? AND status = 'ACTIVE' FOR UPDATE")) {
                    query.setLong(1, id);
                    try (ResultSet rs = query.executeQuery()) {
                        if (!rs.next() || !com.crm.util.PasswordUtil.matches(currentPassword, rs.getString(1))) {
                            connection.rollback();
                            return false;
                        }
                    }
                }
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE users SET password_hash = ?, session_version = session_version + 1,
                            failed_login_attempts = 0, locked_until = NULL WHERE id = ?
                        """)) {
                    update.setString(1, newHash);
                    update.setLong(2, id);
                    update.executeUpdate();
                }
                try (PreparedStatement revoke = connection.prepareStatement(
                        "UPDATE password_reset_tokens SET used_at = NOW() WHERE user_id = ? AND used_at IS NULL")) {
                    revoke.setLong(1, id);
                    revoke.executeUpdate();
                }
                connection.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            }
        }
    }
}
