package com.crm.controller.permissions;

import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.*;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/roles")
public class RoleServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            long id = ((Number) request.getSession(false).getAttribute("userId")).longValue();
            if (!new PermissionService().hasPermission(id, "permission.read")) {
                ResponseUtil.json(response, 403, ApiResponse.error("Không có quyền", null));
                return;
            }
            ResponseUtil.json(response, 200, ApiResponse.success("Lấy role thành công",
                    Map.of("roles", new RoleService().getRoles())));
        } catch (Exception e) {
            ResponseUtil.json(response, 500, ApiResponse.error("Không thể lấy role", null));
        }
    }
}
