package com.crm.service.auth;

import com.crm.dao.auth.PasswordResetDAO;
import com.crm.service.mail.MailService;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Base64;

public class PasswordResetService {

    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final PasswordResetDAO dao =
            new PasswordResetDAO();

    private final MailService mailService =
            new MailService();

    public void forgotPassword(
            String email
    ) throws Exception {

        if (
                email == null ||
                email.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Email là bắt buộc"
            );
        }

        email = email.trim();

        Long userId =
                dao.findActiveUserIdByEmail(
                        email
                );

        /*
         * Không tiết lộ email có tồn tại hay không.
         */
        if (userId == null) {
            return;
        }

        String rawToken =
                generateToken();

        String tokenHash =
                sha256(rawToken);

        Timestamp expiresAt =
                Timestamp.valueOf(
                        LocalDateTime
                                .now()
                                .plusMinutes(
                                        TOKEN_EXPIRY_MINUTES
                                )
                );

        dao.invalidateExistingTokens(
                userId
        );

        dao.saveToken(
                userId,
                tokenHash,
                expiresAt
        );

        try {

            mailService.sendPasswordResetEmail(
                    email,
                    rawToken
            );

        } catch (Exception e) {

            /*
             * Không trả thông tin lỗi SMTP cho client
             * để tránh account enumeration.
             */
            System.err.println(
                    "Không thể gửi email reset password"
                            
            );
        }
    }

    public void resetPassword(
            String rawToken,
            String newPassword,
            String confirmPassword
    ) throws Exception {

        if (
                rawToken == null ||
                rawToken.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Token không hợp lệ"
            );
        }

        validatePassword(
                newPassword,
                confirmPassword
        );

        String tokenHash =
                sha256(
                        rawToken.trim()
                );

        Long userId =
                dao.findValidUserIdByTokenHash(
                        tokenHash
                );

        if (userId == null) {
            throw new IllegalArgumentException(
                    "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn"
            );
        }

        String passwordHash =
                BCrypt.hashpw(
                        newPassword,
                        BCrypt.gensalt(12)
                );

        dao.resetPassword(
                userId,
                passwordHash,
                tokenHash
        );
    }

    private void validatePassword(
            String password,
            String confirmPassword
    ) {

        if (
                password == null ||
                password.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Mật khẩu mới là bắt buộc"
            );
        }

        if (
                !password.equals(
                        confirmPassword
                )
        ) {
            throw new IllegalArgumentException(
                    "Mật khẩu xác nhận không khớp"
            );
        }

        if (password.length() < 8) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 8 ký tự"
            );
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu vượt quá 72 byte");
        }

        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ hoa"
            );
        }

        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ thường"
            );
        }

        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException(
                    "Mật khẩu phải có ít nhất 1 chữ số"
            );
        }
    }

    private String generateToken() {

        byte[] bytes =
                new byte[TOKEN_BYTES];

        new SecureRandom()
                .nextBytes(bytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String sha256(
            String value
    ) throws Exception {

        MessageDigest digest =
                MessageDigest.getInstance(
                        "SHA-256"
                );

        byte[] hash =
                digest.digest(
                        value.getBytes(
                                StandardCharsets.UTF_8
                        )
                );

        StringBuilder result =
                new StringBuilder();

        for (byte b : hash) {
            result.append(
                    String.format(
                            "%02x",
                            b
                    )
            );
        }

        return result.toString();
    }
}
