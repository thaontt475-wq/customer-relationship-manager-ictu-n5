package com.crm.service.auth;

import com.crm.dao.auth.SessionDAO;
import com.crm.dao.permissions.PermissionDAO;
import com.crm.util.PasswordUtil;
import java.sql.SQLException;
import java.util.*;

public class SessionService {
    private final SessionDAO dao = new SessionDAO();
    private final PermissionDAO permissions = new PermissionDAO();

    public Map<String, Object> account(long id) throws SQLException {
        return dao.findAccount(id);
    }

    public Map<String, Object> describe(long id) throws SQLException {
        Map<String, Object> result = account(id);
        if (result == null) throw new IllegalArgumentException("User không tồn tại");
        result.remove("sessionVersion");
        result.put("authenticated", true);
        result.put("roles", permissions.findRolesByUserId(id));
        result.put("permissions", new TreeSet<>(permissions.findByUserId(id)));
        return result;
    }

    public void changePassword(long id, String current, String password, String confirm) throws SQLException {
        if (current == null || current.isBlank()) throw new IllegalArgumentException("Nhập mật khẩu hiện tại");
        if (password == null || password.length() < 8 || !password.matches("(?s).*[A-Z].*")
                || !password.matches("(?s).*[a-z].*") || !password.matches("(?s).*[0-9].*")) {
            throw new IllegalArgumentException("Mật khẩu phải ít nhất 8 ký tự, gồm chữ hoa, chữ thường và số");
        }
        if (!password.equals(confirm)) throw new IllegalArgumentException("Xác nhận mật khẩu không khớp");
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Mật khẩu vượt quá 72 byte");
        }
        if (!dao.changePassword(id, current, PasswordUtil.hash(password))) {
            throw new IllegalArgumentException("Mật khẩu hiện tại không đúng");
        }
    }
}
