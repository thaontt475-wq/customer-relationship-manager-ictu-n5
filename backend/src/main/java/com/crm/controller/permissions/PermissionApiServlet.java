package com.crm.controller.permissions;

import com.crm.model.User;
import com.crm.service.permissions.PermissionService;
import com.crm.service.permissions.PermissionService.AssignmentResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({
        "/api/permissions/users/*",
        "/api/permissions/assign"
})
public class PermissionApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER =
            Logger.getLogger(PermissionApiServlet.class.getName());

    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    private final PermissionService permissionService =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        if (!"/api/permissions/users".equals(request.getServletPath())) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                    false, "Không tìm thấy endpoint", null);
            return;
        }

        Long userId = parseUserIdFromPath(request.getPathInfo());
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    false, "User ID không hợp lệ", null);
            return;
        }

        try {
            User user = permissionService.findUserById(userId);

            if (user == null) {
                writeJson(response, HttpServletResponse.SC_NOT_FOUND,
                        false, "Không tìm thấy người dùng", null);
                return;
            }

            List<Long> roleIds =
                    permissionService.findRoleIdsByUserId(userId);

            String dataScope =
                    user.getDataScope() == null
                            || user.getDataScope().isBlank()
                            ? "SELF"
                            : user.getDataScope();

            PermissionData data =
                    new PermissionData(
                            userId,
                            roleIds,
                            dataScope
                    );

            writeJson(response, HttpServletResponse.SC_OK,
                    true,
                    "Lấy phân quyền người dùng thành công",
                    data);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Unable to load user permissions", e);

            writeJson(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Lỗi hệ thống khi lấy phân quyền",
                    null);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        if (!"/api/permissions/assign".equals(
                request.getServletPath())) {

            writeJson(response,
                    HttpServletResponse.SC_NOT_FOUND,
                    false,
                    "Không tìm thấy endpoint",
                    null);
            return;
        }

        request.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );

        AssignmentRequest body;

        try {
            body = GSON.fromJson(
                    request.getReader(),
                    AssignmentRequest.class
            );
        } catch (JsonSyntaxException e) {
            writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "JSON không hợp lệ",
                    null);
            return;
        }

        if (body == null
                || body.userId() == null
                || body.userId() <= 0
                || body.roleIds() == null
                || body.dataScope() == null
                || body.dataScope().isBlank()) {

            writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Dữ liệu phân quyền không hợp lệ",
                    null);
            return;
        }

        try {
            Object actorValue = request.getSession(false).getAttribute("userId");
            if (!(actorValue instanceof Long actorUserId) || actorUserId <= 0) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
                return;
            }

            AssignmentResult result =
                    permissionService.assign(
                            actorUserId,
                            body.userId(),
                            body.roleIds(),
                            body.dataScope()
                    );

            switch (result) {
                case SUCCESS -> writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        true,
                        "Lưu phân quyền thành công",
                        new PermissionData(
                                body.userId(),
                                body.roleIds(),
                                body.dataScope().trim().toUpperCase()
                        )
                );

                case INVALID_USER,
                     INVALID_ROLE,
                     INVALID_DATA_SCOPE -> writeJson(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        false,
                        "Dữ liệu phân quyền không hợp lệ",
                        null
                );

                case TEAM_REQUIRED -> writeJson(
                        response, HttpServletResponse.SC_CONFLICT, false,
                        "Vai trò Team Lead bắt buộc người dùng phải thuộc một nhóm kinh doanh.", null
                );

                case CANNOT_REVOKE_OWN_ADMIN -> writeJson(
                        response, HttpServletResponse.SC_CONFLICT, false,
                        "Không thể tự gỡ vai trò Admin của chính mình.", null
                );

                case USER_NOT_FOUND -> writeJson(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        false,
                        "Không tìm thấy người dùng",
                        null
                );

                case UPDATE_CONFLICT -> writeJson(
                        response,
                        HttpServletResponse.SC_CONFLICT,
                        false,
                        "Không thể cập nhật phân quyền do dữ liệu đã thay đổi",
                        null
                );
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Unable to assign permissions", e);

            writeJson(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Lỗi hệ thống khi lưu phân quyền",
                    null);
        }
    }

    private Long parseUserIdFromPath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return null;
        }

        String value =
                pathInfo.startsWith("/")
                        ? pathInfo.substring(1)
                        : pathInfo;

        if (value.isBlank() || value.contains("/")) {
            return null;
        }

        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data)
            throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );
        response.setStatus(status);

        GSON.toJson(
                new ApiResponse(
                        success,
                        message,
                        data
                ),
                response.getWriter()
        );
    }

    private record AssignmentRequest(
            Long userId,
            List<Long> roleIds,
            String dataScope) {
    }

    private record PermissionData(
            Long userId,
            List<Long> roles,
            String dataScope) {
    }

    private record ApiResponse(
            boolean success,
            String message,
            Object data) {
    }
}