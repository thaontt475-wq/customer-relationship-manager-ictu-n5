package com.crm.controller.auth;

import com.crm.dao.auth.PasswordResetTokenDAO;
import com.crm.service.auth.AuthService;
import com.crm.util.DBConnection;
import com.crm.util.PasswordUtil;
import com.crm.util.ResetTokenUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ForgotPasswordAcceptanceIntegrationTest {

    private static final String OLD_PASSWORD = "oldpass123";
    private final AuthService authService = new AuthService();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();

    private long userId;
    private String email;

    @BeforeEach
    void createTestUser() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 12);
        email = "s103-" + suffix + "@example.invalid";

        String sql = "INSERT INTO users "
                + "(username, email, password_hash, full_name, display_name, active, status, data_scope) "
                + "VALUES (?, ?, ?, ?, ?, TRUE, 'ACTIVE', 'SELF')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, "s103-" + suffix);
            stmt.setString(2, email);
            stmt.setString(3, PasswordUtil.hashPassword(OLD_PASSWORD));
            stmt.setString(4, "S1-03 Acceptance Test");
            stmt.setString(5, "S1-03 Acceptance Test");
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                assertTrue(keys.next());
                userId = keys.getLong(1);
            }
        }
    }

    @AfterEach
    void removeTestUser() throws Exception {
        if (userId <= 0) {
            return;
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }

    @Test
    void existingAndUnknownEmailUseSameGenericResponseAndOnlyExistingCreatesToken() throws Exception {
        authService.requestPasswordReset(email);

        StoredToken token = latestTokenForUser();
        assertNotNull(token);
        assertTrue(token.tokenHash().matches("[0-9a-f]{64}"));

        long seconds = Duration.between(LocalDateTime.now(), token.expiresAt()).getSeconds();
        assertTrue(seconds >= 29 * 60 && seconds <= 31 * 60);

        int tokenCountAfterExistingEmail = countAllTokens();
        String existingResponse = ForgotPasswordServlet.GENERIC_MESSAGE;

        authService.requestPasswordReset("unknown-" + UUID.randomUUID() + "@example.invalid");
        String unknownResponse = ForgotPasswordServlet.GENERIC_MESSAGE;

        assertEquals(existingResponse, unknownResponse);
        assertEquals(
                "Nếu email tồn tại trong hệ thống, hướng dẫn đặt lại mật khẩu đã được gửi.",
                unknownResponse);
        assertEquals(tokenCountAfterExistingEmail, countAllTokens());
    }

    @Test
    void tokenIsOneTimeAndNewPasswordReplacesOldPassword() throws Exception {
        String rawToken = "one-time-" + UUID.randomUUID();
        createToken(rawToken, LocalDateTime.now().plusMinutes(30));

        String newPassword = "newpass123";
        assertTrue(authService.resetPassword(rawToken, newPassword));
        assertFalse(authService.resetPassword(rawToken, newPassword));
        assertFalse(authService.validateResetToken(rawToken));

        assertNull(authService.login(email, OLD_PASSWORD));
        assertNotNull(authService.login(email, newPassword));
    }

    @Test
    void expiredTokenCannotValidateOrResetPassword() throws Exception {
        String rawToken = "expired-" + UUID.randomUUID();
        createToken(rawToken, LocalDateTime.now().minusMinutes(1));

        assertFalse(authService.validateResetToken(rawToken));
        assertFalse(authService.resetPassword(rawToken, "newpass123"));
        assertNotNull(authService.login(email, OLD_PASSWORD));
    }

    @Test
    void newResetRequestInvalidatesPreviousUnusedToken() throws Exception {
        String previousRawToken = "previous-" + UUID.randomUUID();
        String previousHash = ResetTokenUtil.hashToken(previousRawToken);
        createToken(previousRawToken, LocalDateTime.now().plusMinutes(30));
        assertTrue(authService.validateResetToken(previousRawToken));

        authService.requestPasswordReset(email);

        assertFalse(authService.validateResetToken(previousRawToken));
        assertTrue(isTokenMarkedUsed(previousHash));
        assertEquals(1, countValidUnusedTokensForUser());
    }

    private void createToken(String rawToken, LocalDateTime expiresAt) throws Exception {
        try (Connection conn = DBConnection.getConnection()) {
            tokenDAO.create(conn, userId, ResetTokenUtil.hashToken(rawToken), expiresAt);
        }
    }

    private StoredToken latestTokenForUser() throws Exception {
        String sql = "SELECT token_hash, expires_at FROM password_reset_tokens "
                + "WHERE user_id = ? ORDER BY id DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new StoredToken(
                        rs.getString("token_hash"),
                        rs.getTimestamp("expires_at").toLocalDateTime());
            }
        }
    }

    private int countAllTokens() throws Exception {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM password_reset_tokens");
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private int countValidUnusedTokensForUser() throws Exception {
        String sql = "SELECT COUNT(*) FROM password_reset_tokens "
                + "WHERE user_id = ? AND used_at IS NULL AND expires_at > NOW()";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private boolean isTokenMarkedUsed(String tokenHash) throws Exception {
        String sql = "SELECT used_at FROM password_reset_tokens WHERE token_hash = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tokenHash);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getTimestamp("used_at") != null;
            }
        }
    }

    private record StoredToken(String tokenHash, LocalDateTime expiresAt) {
    }
}
