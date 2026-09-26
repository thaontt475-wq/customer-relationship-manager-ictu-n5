package com.crm.util;

/**
 * Các hằng số session attribute key dùng chung cho cơ chế xác thực và phiên làm việc (CRM-21, CRM-22).
 */
public final class SessionKey {

    private SessionKey() {
        // Utility class, không khởi tạo
    }

    /**
     * Key lưu thông tin người dùng hiện tại đã xác thực (User model, DTO hoặc Map).
     */
    public static final String CURRENT_USER = "currentUser";

    /**
     * Key lưu danh sách vai trò / quyền của người dùng (Collection / List).
     */
    public static final String ROLES = "roles";

    /**
     * Key lưu thời điểm hết hạn của phiên làm việc (nếu có cấu hình tùy biến).
     */
    public static final String EXPIRES_AT = "expiresAt";
}
