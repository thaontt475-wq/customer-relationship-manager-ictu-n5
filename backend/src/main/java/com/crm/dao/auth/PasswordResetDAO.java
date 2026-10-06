package com.crm.dao.auth;

import com.crm.config.DatabaseConfig;

import java.sql.*;

public class PasswordResetDAO {

    public Long findActiveUserIdByEmail(String email)
            throws SQLException {

        String sql = """
                SELECT id
                FROM users
                WHERE LOWER(email) = LOWER(?)
                  AND status = 'ACTIVE'
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, email);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("id");
                }

                return null;
            }
        }
    }

    public void invalidateExistingTokens(long userId)
            throws SQLException {

        String sql = """
                UPDATE password_reset_tokens
                SET used_at = CURRENT_TIMESTAMP
                WHERE user_id = ?
                  AND used_at IS NULL
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);
            statement.executeUpdate();
        }
    }

    public void saveToken(
            long userId,
            String tokenHash,
            Timestamp expiresAt
    ) throws SQLException {

        String sql = """
                INSERT INTO password_reset_tokens(
                    user_id,
                    token_hash,
                    expires_at
                )
                VALUES (?, ?, ?)
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setLong(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, expiresAt);

            statement.executeUpdate();
        }
    }

    public Long findValidUserIdByTokenHash(
            String tokenHash
    ) throws SQLException {

        String sql = """
                SELECT user_id
                FROM password_reset_tokens
                WHERE token_hash = ?
                  AND used_at IS NULL
                  AND expires_at > CURRENT_TIMESTAMP
                LIMIT 1
                """;

        try (
                Connection connection =
                        DatabaseConfig.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {

            statement.setString(1, tokenHash);

            try (ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    return rs.getLong("user_id");
                }

                return null;
            }
        }
    }

    public void resetPassword(
            long userId,
            String passwordHash,
            String tokenHash
    ) throws SQLException {

        Connection connection = null;

        try {

            connection =
                    DatabaseConfig.getConnection();

            connection.setAutoCommit(false);

            try (
                    PreparedStatement updatePassword =
                            connection.prepareStatement("""
                                    UPDATE users
                                    SET password_hash = ?,
                                        failed_login_attempts = 0,
                                        locked_until = NULL,
                                        session_version = session_version + 1,
                                        updated_at = CURRENT_TIMESTAMP
                                    WHERE id = ? AND status = 'ACTIVE'
                                    """);

                    PreparedStatement consumeToken =
                            connection.prepareStatement("""
                                    UPDATE password_reset_tokens
                                    SET used_at = CURRENT_TIMESTAMP
                                    WHERE token_hash = ?
                                      AND used_at IS NULL
                                      AND expires_at > CURRENT_TIMESTAMP
                                    """);

                    PreparedStatement invalidateOthers =
                            connection.prepareStatement("""
                                    UPDATE password_reset_tokens
                                    SET used_at = CURRENT_TIMESTAMP
                                    WHERE user_id = ?
                                      AND used_at IS NULL
                                    """)
            ) {

                updatePassword.setString(
                        1,
                        passwordHash
                );

                updatePassword.setLong(
                        2,
                        userId
                );

                if (updatePassword.executeUpdate() != 1) {
                    throw new SQLException(
                            "Không thể cập nhật mật khẩu"
                    );
                }

                consumeToken.setString(
                        1,
                        tokenHash
                );

                if (consumeToken.executeUpdate() != 1) {
                    throw new IllegalArgumentException(
                            "Token không còn hợp lệ"
                    );
                }

                invalidateOthers.setLong(
                        1,
                        userId
                );

                invalidateOthers.executeUpdate();

                connection.commit();
            }

        } catch (SQLException | IllegalArgumentException e) {

            if (connection != null) {
                try {
                    connection.rollback();
                } catch (SQLException ignored) {
                }
            }

            throw e;

        } finally {

            if (connection != null) {

                try {
                    connection.setAutoCommit(true);
                } catch (SQLException ignored) {
                }

                try {
                    connection.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
