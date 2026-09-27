package com.crm.controller.permissions;

import com.crm.model.User;
import com.crm.service.permissions.PermissionService;
import com.crm.service.permissions.PermissionService.AssignmentResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({
        "/permissions",
        "/permissions/assign"
})
public class PermissionServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(PermissionServlet.class.getName());
    private static final String PERMISSION_JSP = "/jsp/permissions/role-permission.jsp";

    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        if (!"/permissions".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long selectedUserId = null;
        String userIdParam = request.getParameter("viewUserId");
        if (userIdParam != null && !userIdParam.isBlank()) {
            selectedUserId = parsePositiveLong(userIdParam);
            if (selectedUserId == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
        }

        try {
            loadPage(request, response, selectedUserId, HttpServletResponse.SC_OK, null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load role permission page", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");

        if (!"/permissions/assign".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long userId = parsePositiveLong(request.getParameter("userId"));
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        List<Long> roleIds = parseRoleIds(request.getParameterValues("roleIds"));
        if (roleIds == null) {
            forwardError(request, response, userId,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Danh sách vai trò không hợp lệ.");
            return;
        }

        String dataScope = request.getParameter("dataScope");

        try {
            AssignmentResult result = permissionService.assign(userId, roleIds, dataScope);
            switch (result) {
                case SUCCESS -> response.sendRedirect(request.getContextPath()
                        + "/permissions?viewUserId=" + userId + "&saved=1");
                case INVALID_USER, INVALID_ROLE, INVALID_DATA_SCOPE -> forwardError(
                        request, response, userId, HttpServletResponse.SC_BAD_REQUEST,
                        "Dữ liệu phân quyền không hợp lệ.");
                case USER_NOT_FOUND -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
                case UPDATE_CONFLICT -> forwardError(
                        request, response, userId, HttpServletResponse.SC_CONFLICT,
                        "Không thể cập nhật phân quyền do dữ liệu đã thay đổi.");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to assign roles and data scope", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void loadPage(HttpServletRequest request, HttpServletResponse response,
                          Long selectedUserId, int status, String error)
            throws SQLException, ServletException, IOException {
        request.setAttribute("users", permissionService.findAllUsers());
        request.setAttribute("roles", permissionService.findAllRoles());
        request.setAttribute("teams", List.of());

        if (selectedUserId != null) {
            User selectedUser = permissionService.findUserById(selectedUserId);
            if (selectedUser == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("selectedUser", selectedUser);
            request.setAttribute("userRoles", permissionService.findRoleIdsByUserId(selectedUserId));
            request.setAttribute("dataScope",
                    selectedUser.getDataScope() == null || selectedUser.getDataScope().isBlank()
                            ? "SELF"
                            : selectedUser.getDataScope());
        } else {
            request.setAttribute("userRoles", List.of());
            request.setAttribute("dataScope", "SELF");
        }

        if ("1".equals(request.getParameter("saved"))) {
            request.setAttribute("message", "Lưu phân quyền thành công.");
        }
        if (error != null) {
            request.setAttribute("error", error);
        }

        response.setStatus(status);
        request.getRequestDispatcher(PERMISSION_JSP).forward(request, response);
    }

    private void forwardError(HttpServletRequest request, HttpServletResponse response,
                              long userId, int status, String error)
            throws ServletException, IOException {
        try {
            loadPage(request, response, userId, status, error);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to reload role permission page", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private List<Long> parseRoleIds(String[] values) {
        if (values == null || values.length == 0) {
            return List.of();
        }
        List<Long> roleIds = new ArrayList<>();
        for (String value : values) {
            Long roleId = parsePositiveLong(value);
            if (roleId == null) {
                return null;
            }
            roleIds.add(roleId);
        }
        return roleIds;
    }

    private Long parsePositiveLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value);
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
