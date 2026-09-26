package com.crm.dao.auth;

import com.crm.model.PasswordResetToken;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class PasswordResetTokenDAO {

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /** Map một ResultSet row thành PasswordResetToken (cursor phải đã ở đúng row). */
    private PasswordResetToken mapToken(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getLong("id"));
        token.setUserId(rs.getLong("user_id"));
        token.setTokenHash(rs.getString("token_hash"));
        token.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        Timestamp usedTs = rs.getTimestamp("used_at");
        token.setUsedAt(usedTs != null ? usedTs.toLocalDateTime() : null);
        return token;
    }

    // ------------------------------------------------------------------ //
    //  Public methods                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Invalidate tất cả token chưa dùng của một user bằng cách set used_at = NOW().
     */
    public void invalidateUnusedTokens(Connection conn, long userId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = NOW() WHERE user_id = ? AND used_at IS NULL";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }

    /**
     * Insert token mới.
     */
    public void create(Connection conn, long userId, String tokenHash, java.time.LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, tokenHash);
            stmt.setTimestamp(3, Timestamp.valueOf(expiresAt));
            stmt.executeUpdate();
        }
    }

    /**
     * Tìm token hợp lệ (chưa dùng, chưa hết hạn) — KHÔNG có FOR UPDATE.
     * Dùng cho GET validation (read-only, không transaction).
     */
    public PasswordResetToken findValidToken(Connection conn, String tokenHash) throws SQLException {
        String sql = "SELECT id, user_id, token_hash, expires_at, used_at " +
                     "FROM password_reset_tokens " +
                     "WHERE token_hash = ? AND used_at IS NULL AND expires_at > NOW()";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tokenHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapToken(rs);
                }
            }
        }
        return null;
    }

    /**
     * Tìm token hợp lệ và lock row (FOR UPDATE).
     * CHỈ dùng trong AuthService.resetPassword() — trong transaction với setAutoCommit(false).
     */
    public PasswordResetToken findValidTokenForUpdate(Connection conn, String tokenHash) throws SQLException {
        String sql = "SELECT id, user_id, token_hash, expires_at, used_at " +
                     "FROM password_reset_tokens " +
                     "WHERE token_hash = ? AND used_at IS NULL AND expires_at > NOW() FOR UPDATE";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tokenHash);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapToken(rs);
                }
            }
        }
        return null;
    }

    /**
     * Đánh dấu token đã dùng.
     */
    public void markUsed(Connection conn, long tokenId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = NOW() WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, tokenId);
            stmt.executeUpdate();
        }
    }
}
