package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.crm.service.auth.AuthService.ChangePasswordResult;
import com.crm.util.SessionRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet({"/change-password", "/api/auth/change-password"})
public class ChangePasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!"/change-password".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        if (resolveUserId(request) == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        request.getRequestDispatcher("/jsp/auth/change-password.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long userId = resolveUserId(request);
        if (userId == null) {
            if ("/api/auth/change-password".equals(request.getServletPath())) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                        false, "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login");
            }
            return;
        }

        String currentPassword;
        String newPassword;

        String contentType = request.getContentType();
        if (contentType != null
                && contentType.toLowerCase(java.util.Locale.ROOT)
                        .startsWith("application/json")) {
            try {
                ChangePasswordRequest body = GSON.fromJson(
                        request.getReader(),
                        ChangePasswordRequest.class
                );
                currentPassword = body == null ? null : body.currentPassword;
                newPassword = body == null ? null : body.newPassword;
            } catch (com.google.gson.JsonSyntaxException e) {
                writeResponse(
                        request,
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        false,
                        "Dữ liệu yêu cầu không hợp lệ",
                        null
                );
                return;
            }
        } else {
            currentPassword = request.getParameter("currentPassword");
            newPassword = request.getParameter("newPassword");
        }

        ChangePasswordResult result;
        try {
            result = authService.changePassword(
                    userId,
                    currentPassword,
                    newPassword
            );
        } catch (SQLException e) {
            getServletContext().log("Change password failed", e);
            writeResponse(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Không thể đổi mật khẩu lúc này. Vui lòng thử lại sau.",
                    null);
            return;
        }

        switch (result) {
            case SUCCESS -> {
                HttpSession currentSession = request.getSession(false);
                int revokedSessions =
                        SessionRegistry.invalidateOtherSessions(userId, currentSession);

                Map<String, Object> data = new LinkedHashMap<>();
                data.put("revokedSessions", revokedSessions);

                writeResponse(request, response,
                        HttpServletResponse.SC_OK,
                        true,
                        "Đổi mật khẩu thành công",
                        data);
            }
            case CURRENT_PASSWORD_REQUIRED -> writeResponse(
                    request, response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Vui lòng nhập mật khẩu hiện tại",
                    null);
            case CURRENT_PASSWORD_INCORRECT -> writeResponse(
                    request, response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Mật khẩu hiện tại không đúng",
                    null);
            case INVALID_NEW_PASSWORD -> writeResponse(
                    request, response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Mật khẩu mới phải có ít nhất 8 ký tự, gồm chữ và số",
                    null);
            case USER_NOT_FOUND -> writeResponse(
                    request, response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Phiên đăng nhập không còn hợp lệ",
                    null);
        }
    }

    private Long resolveUserId(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return null;
        }

        if (session == null) {
            return null;
        }

        Object value;
        try {
            value = session.getAttribute("userId");
        } catch (IllegalStateException e) {
            return null;
        }

        if (value instanceof Number number) {
            long id = number.longValue();
            return id > 0 ? id : null;
        }

        if (value instanceof String text) {
            try {
                long id = Long.parseLong(text);
                return id > 0 ? id : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        return null;
    }

    private void writeResponse(HttpServletRequest request,
                               HttpServletResponse response,
                               int status,
                               boolean success,
                               String message,
                               Object data)
            throws ServletException, IOException {

        if ("/api/auth/change-password".equals(request.getServletPath())) {
            writeJson(response, status, success, message, data);
            return;
        }

        response.setStatus(status);

        if (success) {
            request.setAttribute("message", message);
        } else {
            request.setAttribute("error", message);
        }

        request.getRequestDispatcher("/jsp/auth/change-password.jsp")
                .forward(request, response);
    }

    private void writeJson(HttpServletResponse response,
                           int status,
                           boolean success,
                           String message,
                           Object data)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        GSON.toJson(
                new ApiResponse(success, message, data),
                response.getWriter()
        );
    }

    private static final class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
    private record ApiResponse(
            boolean success,
            String message,
            Object data) {
    }
}