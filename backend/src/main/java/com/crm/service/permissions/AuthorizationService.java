package com.crm.service.permissions;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class AuthorizationService {
    private final PermissionService permissions = new PermissionService();
    private final DataScopeService scopes = new DataScopeService();

    public static long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Number id)) {
            throw new SecurityException("Phiên đăng nhập không hợp lệ");
        }
        return id.longValue();
    }

    public void requirePermission(HttpServletRequest request, String permission) throws Exception {
        if (!permissions.hasPermission(currentUserId(request), permission)) {
            throw new SecurityException("Bạn không có quyền thực hiện thao tác này.");
        }
    }

    public DataScopeContext resolveScope(HttpServletRequest request, String module, String action)
            throws Exception {
        return scopes.resolve(currentUserId(request), module, action);
    }

    public boolean canAccessOwner(DataScopeContext scope, long ownerId) {
        return scope.canAccessOwner(ownerId);
    }

    public void requireRecord(DataScopeContext scope, long ownerId) {
        if (!canAccessOwner(scope, ownerId)) {
            throw new SecurityException("Bạn không có quyền truy cập dữ liệu này.");
        }
    }
}
