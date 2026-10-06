package com.crm.dao.users;

import com.crm.config.DatabaseConfig;
import com.crm.model.users.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public User findByEmail(String email) throws SQLException {

        String sql = """
                SELECT
                    id,
                    full_name,
                    email,
                    password_hash,
                    status,
                    failed_login_attempts,
                    locked_until,
                    session_version
                FROM users
                WHERE email = ?
                LIMIT 1
                """;

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {

            statement.setString(1, email);

            try (ResultSet rs = statement.executeQuery()) {

                if (!rs.next()) {
                    return null;
                }

                User user = new User();

                user.setId(rs.getLong("id"));
                user.setSessionVersion(rs.getLong("session_version"));
                user.setFullName(rs.getString("full_name"));
                user.setEmail(rs.getString("email"));
                user.setPasswordHash(rs.getString("password_hash"));
                user.setStatus(rs.getString("status"));
                user.setFailedLoginAttempts(
                        rs.getInt("failed_login_attempts")
                );

                Timestamp lockedUntil =
                        rs.getTimestamp("locked_until");

                if (lockedUntil != null) {
                    user.setLockedUntil(
                            lockedUntil.toLocalDateTime()
                    );
                }

                user.setRoles(
                        findRoles(connection, user.getId())
                );

                return user;
            }
        }
    }

    private List<String> findRoles(
            Connection connection,
            long userId
    ) throws SQLException {

        String sql = """
                SELECT r.code
                FROM roles r
                JOIN user_roles ur
                    ON ur.role_id = r.id
                WHERE ur.user_id = ?
                ORDER BY r.code
                """;

        List<String> roles = new ArrayList<>();

        try (
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);

            try (ResultSet rs = statement.executeQuery()) {

                while (rs.next()) {
                    roles.add(rs.getString("code"));
                }
            }
        }

        return roles;
    }

    public void updateLoginState(
            long userId,
            int attempts,
            Timestamp lockedUntil
    ) throws SQLException {

        String sql = """
                UPDATE users
                SET
                    failed_login_attempts = ?,
                    locked_until = ?
                WHERE id = ?
                """;

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setInt(1, attempts);

            if (lockedUntil == null) {
                statement.setNull(
                        2,
                        Types.TIMESTAMP
                );
            } else {
                statement.setTimestamp(
                        2,
                        lockedUntil
                );
            }

            statement.setLong(3, userId);

            statement.executeUpdate();
        }
    }

    public boolean existsById(long id)
            throws SQLException {

        String sql =
                "SELECT 1 FROM users WHERE id = ? AND status <> 'DELETED'";

        try (
                Connection connection = DatabaseConfig.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, id);

            try (ResultSet rs = statement.executeQuery()) {
                return rs.next();
            }
        }
    }
}
