package com.crm.service.users;

import com.crm.dao.users.UserManagementDAO;
import com.crm.dto.users.UserWriteRequest;
import com.crm.util.PasswordPolicy;

import java.util.*;

public class UserManagementService {

    private final UserManagementDAO dao =
            new UserManagementDAO();

    public Map<String, Object> search(
            String keyword,
            String status,
            int page,
            int size
    ) throws Exception {

        if (page < 1) {
            throw new IllegalArgumentException(
                    "page phải >= 1"
            );
        }

        if (
                size < 1 ||
                size > 100
        ) {
            throw new IllegalArgumentException(
                    "size phải từ 1 đến 100"
            );
        }

        long total =
                dao.count(
                        keyword,
                        status
                );

        int totalPages =
                (int) Math.ceil(
                        (double) total / size
                );

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put(
                "items",
                dao.search(
                        keyword,
                        status,
                        page,
                        size
                )
        );

        data.put("page", page);
        data.put("size", size);
        data.put("totalItems", total);
        data.put(
                "totalPages",
                totalPages
        );

        return data;
    }

    public Map<String, Object> get(
            long id
    ) throws Exception {

        return dao.findById(id);
    }

    public Map<String, Object> create(
            UserWriteRequest body
    ) throws Exception {

        validateCreate(body);

        String email =
                body.getEmail()
                        .trim()
                        .toLowerCase();

        if (
                dao.emailExists(
                        email,
                        null
                )
        ) {
            throw new IllegalStateException(
                    "Email đã tồn tại"
            );
        }

        String status =
                normalizeStatus(
                        body.getStatus()
                );

        long id =
                dao.create(
                        body.getFullName().trim(),
                        email,
                        body.getPassword(),
                        status
                );

        return dao.findById(id);
    }

    public Map<String, Object> update(
            long id,
            UserWriteRequest body,
            long currentUserId
    ) throws Exception {

        Map<String, Object> existing =
                dao.findById(id);

        if (existing == null) {
            return null;
        }

        if (
                body == null ||
                body.getFullName() == null ||
                body.getFullName().isBlank() ||
                body.getEmail() == null ||
                body.getEmail().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "fullName và email là bắt buộc"
            );
        }

        String email =
                body.getEmail()
                        .trim()
                        .toLowerCase();

        if (
                dao.emailExists(
                        email,
                        id
                )
        ) {
            throw new IllegalStateException(
                    "Email đã tồn tại"
            );
        }

        String status =
                body.getStatus() == null
                        ? (String) existing.get("status")
                        : normalizeStatus(
                                body.getStatus()
                        );

        if (id == currentUserId && !"ACTIVE".equals(status)) {
            throw new IllegalArgumentException("Không được tự khóa hoặc ngừng hoạt động chính mình");
        }
        validateIdentity(body);
        dao.update(
                id,
                body.getFullName().trim(),
                email,
                status
        );

        Map<String, Object> updated = dao.findById(id);

        try {
            com.crm.service.audit.AuditLogService auditService =
                    new com.crm.service.audit.AuditLogService();
            Map<String, Object> before = new LinkedHashMap<>();
            before.put("fullName", existing.get("fullName"));
            before.put("email", existing.get("email"));
            before.put("status", existing.get("status"));

            Map<String, Object> after = new LinkedHashMap<>();
            after.put("fullName", updated != null ? updated.get("fullName") : body.getFullName());
            after.put("email", updated != null ? updated.get("email") : email);
            after.put("status", updated != null ? updated.get("status") : status);

            auditService.log(
                    currentUserId,
                    "USER",
                    String.valueOf(id),
                    "UPDATE_USER",
                    "Cập nhật thông tin người dùng",
                    com.crm.util.JsonUtil.getGson().toJson(before),
                    com.crm.util.JsonUtil.getGson().toJson(after)
            );
        } catch (Exception ignored) {}

        return updated;
    }

    public void delete(
            long id,
            long currentUserId
    ) throws Exception {

        if (id == currentUserId) {
            throw new IllegalArgumentException(
                    "Không được xóa chính mình"
            );
        }

        if (dao.findById(id) == null) {
            throw new NoSuchElementException(
                    "User không tồn tại"
            );
        }

        dao.softDelete(id);
    }

    private void validateCreate(
            UserWriteRequest body
    ) {

        validateIdentity(body);

        if (
                body == null ||
                body.getFullName() == null ||
                body.getFullName().isBlank() ||
                body.getEmail() == null ||
                body.getEmail().isBlank()
        ) {
            throw new IllegalArgumentException(
                    "fullName và email là bắt buộc"
            );
        }

        if (
                !PasswordPolicy.isValid(
                        body.getPassword()
                )
        ) {
            throw new IllegalArgumentException(
                    "Password phải ít nhất 8 ký tự, gồm chữ và số"
            );
        }
    }

    private String normalizeStatus(
            String status
    ) {

        if (
                status == null ||
                status.isBlank()
        ) {
            return "ACTIVE";
        }

        status =
                status.trim()
                        .toUpperCase();

        if (
                !status.equals("ACTIVE") &&
                !status.equals("LOCKED") &&
                !status.equals("INACTIVE")
        ) {
            throw new IllegalArgumentException(
                    "status không hợp lệ"
            );
        }

        return status;
    }

    private void validateIdentity(UserWriteRequest body) {
        if (body == null || body.getFullName() == null || body.getFullName().isBlank()
                || body.getFullName().trim().length() > 255 || body.getEmail() == null
                || body.getEmail().trim().length() > 255
                || !body.getEmail().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Họ tên hoặc email không hợp lệ");
        }
    }
}
