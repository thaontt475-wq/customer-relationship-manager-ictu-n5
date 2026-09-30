package com.crm.service.users;

import com.crm.dao.users.UserDAO;
import com.crm.model.User;
import com.crm.util.DBConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Service managing user profile view and self-update (CRM-35).
 * Enforces business logic:
 * 1. Allowed update fields: full_name, phone, signature.
 * 2. Forbidden update fields: email, team, role (blocked in backend).
 * 3. Vietnamese phone number validation: starts with 0 (or +84), valid carrier prefixes (3, 5, 7, 8, 9), 10 digits total.
 */
public class ProfileService {
    private static final Logger LOGGER = Logger.getLogger(ProfileService.class.getName());

    /**
     * Regex matching valid 10-digit Vietnamese phone numbers:
     * Examples: 0912345678, 0387654321, 0791122334, 0855667788, 0566778899,
     * or with international prefix: +84912345678, +84387654321.
     */
    public static final Pattern VN_PHONE_PATTERN = Pattern.compile("^(0[35789])[0-9]{8}$");
    public static final Pattern VN_PHONE_INTL_PATTERN = Pattern.compile("^(\\+84[35789])[0-9]{8}$");

    private final UserDAO userDAO;

    public ProfileService() {
        this.userDAO = new UserDAO();
    }

    public ProfileService(UserDAO userDAO) {
        this.userDAO = userDAO != null ? userDAO : new UserDAO();
    }

    /**
     * Retrieve complete user profile by user ID.
     */
    public User getUserProfile(long userId) throws SQLException {
        if (userId <= 0) {
            return null;
        }
        try (Connection conn = DBConnection.getConnection()) {
            return userDAO.findUserProfileWithRoles(conn, userId);
        }
    }

    /**
     * Update user self profile (CRM-35).
     *
     * Business rules:
     * - full_name: required, non-blank, max 255 chars.
     * - phone: optional. If present, MUST be a valid Vietnamese phone number.
     * - signature: optional, max 2000 chars.
     * - Email, Team, and Role are completely ignored and preserved.
     *
     * @param userId    current user's ID
     * @param fullName  new full name
     * @param phone     new phone number
     * @param signature new email signature
     * @return updated User entity
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException             if database error occurs
     */
    public User updateProfile(long userId, String fullName, String phone, String signature)
            throws SQLException, IllegalArgumentException {

        if (userId <= 0) {
            throw new IllegalArgumentException("ID người dùng không hợp lệ.");
        }

        // 1. Validate full name
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Họ và tên không được để trống.");
        }
        String cleanFullName = fullName.trim();
        if (cleanFullName.length() > 255) {
            throw new IllegalArgumentException("Họ và tên không được vượt quá 255 ký tự.");
        }

        // 2. Validate Vietnamese phone number
        String cleanPhone = normalizeAndValidatePhone(phone);

        // 3. Normalize signature
        String cleanSignature = signature != null ? signature.trim() : "";
        if (cleanSignature.length() > 2000) {
            throw new IllegalArgumentException("Chữ ký email không được vượt quá 2000 ký tự.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                User existing = userDAO.findByIdForUpdate(conn, userId);
                if (existing == null) {
                    conn.rollback();
                    throw new IllegalArgumentException("Không tìm thấy thông tin tài khoản người dùng.");
                }

                // Execute SQL UPDATE strictly updating ONLY full_name, display_name, phone, signature
                // Email, team_id, and roles are NEVER modified here
                int updated = userDAO.updateUserSelfProfile(conn, userId, cleanFullName, cleanPhone, cleanSignature);
                if (updated <= 0) {
                    conn.rollback();
                    throw new SQLException("Không thể cập nhật hồ sơ cá nhân.");
                }

                conn.commit();

                // Return fresh profile with roles and team
                return userDAO.findUserProfileWithRoles(conn, userId);

            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    /**
     * Validate Vietnamese phone format:
     * Accepts:
     * - empty / null (returns empty or null)
     * - 03x, 05x, 07x, 08x, 09x followed by 8 digits
     * - +843x, +845x, +847x, +848x, +849x followed by 8 digits (normalized to 0x)
     */
    public String normalizeAndValidatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return "";
        }

        String raw = phone.trim().replaceAll("[\\s.-]", "");

        // Convert international format +84xxxxxxxxx or 84xxxxxxxxx to standard 0xxxxxxxxx
        if (raw.startsWith("+84") && raw.length() == 12) {
            raw = "0" + raw.substring(3);
        } else if (raw.startsWith("84") && raw.length() == 11) {
            raw = "0" + raw.substring(2);
        }

        if (!isValidVietnamesePhone(raw)) {
            throw new IllegalArgumentException("Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam hợp lệ (ví dụ: 0912345678 hoặc +84912345678).");
        }

        return raw;
    }

    /**
     * Check if a given phone string matches the Vietnamese phone regex.
     */
    public boolean isValidVietnamesePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return false;
        }
        String p = phone.trim().replaceAll("[\\s.-]", "");
        if (p.startsWith("+84") && p.length() == 12) {
            p = "0" + p.substring(3);
        } else if (p.startsWith("84") && p.length() == 11) {
            p = "0" + p.substring(2);
        }
        return VN_PHONE_PATTERN.matcher(p).matches();
    }
}
