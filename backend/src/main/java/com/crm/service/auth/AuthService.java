package com.crm.service.auth;

import com.crm.dao.auth.PasswordResetTokenDAO;
import com.crm.dao.users.UserDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;
import com.crm.util.PasswordUtil;
import com.crm.util.ResetTokenUtil;
import com.crm.service.email.EmailService;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthService {
    private static final Logger LOGGER = Logger.getLogger(AuthService.class.getName());
    private static final LoginAttemptGuard LOGIN_ATTEMPTS = new LoginAttemptGuard();
    private final UserDAO userDAO = new UserDAO();
    private final PasswordResetTokenDAO tokenDAO = new PasswordResetTokenDAO();
    private final EmailService emailService = new EmailService();

    /** Authentication result excludes credentials and password hashes. */
    public record LoginResult(long userId, String displayName, java.util.List<String> roles) {
        public LoginResult { roles = java.util.List.copyOf(roles); }
    }

    public LoginResult login(String email, String password) throws SQLException {
        if (email == null || email.isBlank() || password == null || password.isBlank()) return null;
        String normalizedEmail = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (LOGIN_ATTEMPTS.isBlocked(normalizedEmail)) return null;

        User user = userDAO.findForLogin(normalizedEmail);
        if (user == null) {
            LOGIN_ATTEMPTS.recordFailure(normalizedEmail);
            return null;
        }
        if (!user.isActive() || !"ACTIVE".equals(user.getStatus())) return null;
        try {
            if (!PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                LOGIN_ATTEMPTS.recordFailure(normalizedEmail);
                return null;
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid stored BCrypt hash for user id " + user.getId());
            LOGIN_ATTEMPTS.recordFailure(normalizedEmail);
            return null;
        }
        LOGIN_ATTEMPTS.recordSuccess(normalizedEmail);
        return new LoginResult(user.getId(), user.getDisplayName(),
                user.getRoles().stream().map(com.crm.model.Role::getName).toList());
    }

    /**
     * Request a password reset.
     * Luôn trả void — caller phải luôn hiển thị generic success message
     * bất kể email có tồn tại hay không (chống email enumeration).
     */
    public void requestPasswordReset(String email) {
        Connection conn = null;
        boolean committed = false;
        String rawTokenToSend = null;
        String emailToSend = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            User user = userDAO.findByEmail(conn, email);

            if (user != null) {
                String rawToken = ResetTokenUtil.generateRawToken();
                String tokenHash = ResetTokenUtil.hashToken(rawToken);
                LocalDateTime expiresAt = ResetTokenUtil.calculateExpiry();

                // Hủy tất cả token cũ chưa dùng của user
                tokenDAO.invalidateUnusedTokens(conn, user.getId());
                // Tạo token mới
                tokenDAO.create(conn, user.getId(), tokenHash, expiresAt);

                conn.commit();
                committed = true;

                // Lưu lại để gửi email SAU khi commit — không log raw token
                rawTokenToSend = rawToken;
                emailToSend = user.getEmail();
            } else {
                conn.rollback();
                committed = true; // không có gì để rollback thêm
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during password reset request", e);
            if (conn != null && !committed) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback requestPasswordReset", rollbackEx);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error during password reset request", e);
            if (conn != null && !committed) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback requestPasswordReset (unexpected)", rollbackEx);
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    LOGGER.log(Level.WARNING, "Failed to close connection in requestPasswordReset", closeEx);
                }
            }
        }

        // Gửi email hoàn toàn ngoài transaction
        if (rawTokenToSend != null && emailToSend != null) {
            emailService.sendPasswordResetEmail(emailToSend, rawTokenToSend);
        }
    }

    /**
     * Validate raw token từ URL param.
     * Dùng findValidToken() — read-only, KHÔNG FOR UPDATE.
     *
     * @return true nếu token tồn tại, chưa dùng và chưa hết hạn.
     */
    public boolean validateResetToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        String tokenHash = ResetTokenUtil.hashToken(rawToken);
        try (Connection conn = DBConnection.getConnection()) {
            return tokenDAO.findValidToken(conn, tokenHash) != null;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validating reset token", e);
            return false;
        }
    }

    /**
     * Đặt lại mật khẩu bằng raw token và mật khẩu mới.
     *
     * @return true nếu thành công, false nếu thất bại.
     */
    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        if (!PasswordUtil.isValidPassword(newPassword)) {
            return false;
        }

        String tokenHash = ResetTokenUtil.hashToken(rawToken);
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Lock token row — FOR UPDATE đảm bảo không có race condition
            var token = tokenDAO.findValidTokenForUpdate(conn, tokenHash);
            if (token == null) {
                conn.rollback();
                return false;
            }

            // Cập nhật mật khẩu
            String newHash = PasswordUtil.hashPassword(newPassword);
            userDAO.updatePasswordHash(conn, token.getUserId(), newHash);

            // Đánh dấu token đã dùng
            tokenDAO.markUsed(conn, token.getId());

            conn.commit();
            return true;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error resetting password", e);
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    LOGGER.log(Level.SEVERE, "Failed to rollback resetPassword", rollbackEx);
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException closeEx) {
                    LOGGER.log(Level.WARNING, "Failed to close connection in resetPassword", closeEx);
                }
            }
        }
    }
    public ChangePasswordResult changePassword(long userId, String currentPassword, String newPassword)
            throws SQLException {

        if (userId <= 0) {
            return ChangePasswordResult.USER_NOT_FOUND;
        }
        if (currentPassword == null || currentPassword.isBlank()) {
            return ChangePasswordResult.CURRENT_PASSWORD_REQUIRED;
        }
        if (!PasswordUtil.isValidPassword(newPassword)) {
            return ChangePasswordResult.INVALID_NEW_PASSWORD;
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String currentHash = userDAO.findPasswordHashById(conn, userId);
                if (currentHash == null) {
                    conn.rollback();
                    return ChangePasswordResult.USER_NOT_FOUND;
                }

                boolean matches;
                try {
                    matches = PasswordUtil.verifyPassword(currentPassword, currentHash);
                } catch (IllegalArgumentException e) {
                    conn.rollback();
                    return ChangePasswordResult.CURRENT_PASSWORD_INCORRECT;
                }

                if (!matches) {
                    conn.rollback();
                    return ChangePasswordResult.CURRENT_PASSWORD_INCORRECT;
                }

                String newHash = PasswordUtil.hashPassword(newPassword);
                userDAO.updatePasswordHash(conn, userId, newHash);
                conn.commit();
                return ChangePasswordResult.SUCCESS;

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }
                throw e;
            }
        }
    }

    public enum ChangePasswordResult {
        SUCCESS,
        CURRENT_PASSWORD_REQUIRED,
        CURRENT_PASSWORD_INCORRECT,
        INVALID_NEW_PASSWORD,
        USER_NOT_FOUND
    }
}
