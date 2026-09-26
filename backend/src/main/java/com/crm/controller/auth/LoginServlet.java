package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.crm.util.SessionKey;
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
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet({"/login", "/api/auth/login"})
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Gson GSON =
            new GsonBuilder().serializeNulls().create();

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!"/login".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        request.getRequestDispatcher("/jsp/auth/login.jsp")
                .forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if ("/api/auth/login".equals(request.getServletPath())) {
            handleApiLogin(request, response);
            return;
        }

        handleViewLogin(request, response);
    }

    /**
     * View adapter cho form JSP.
     * Thành công redirect về trang hiện có.
     */
    private void handleViewLogin(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (isMissing(email) || isMissing(password)) {
            request.setAttribute("email", email);
            request.setAttribute(
                    "error",
                    "Vui lòng nhập email và mật khẩu"
            );
            forwardLogin(request, response);
            return;
        }

        AuthService.LoginResult result;

        try {
            result = authService.login(email, password);
        } catch (SQLException e) {
            getServletContext().log(
                    "Login failed due to database error",
                    e
            );

            response.setStatus(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            );

            request.setAttribute(
                    "error",
                    "Không thể đăng nhập lúc này. Vui lòng thử lại sau."
            );

            forwardLogin(request, response);
            return;
        }

        if (result == null) {
            request.setAttribute("email", email);
            request.setAttribute(
                    "error",
                    "Email hoặc mật khẩu không đúng"
            );
            forwardLogin(request, response);
            return;
        }

        createAuthenticatedSession(request, result);

        response.sendRedirect(
                request.getContextPath() + "/html/index.html"
        );
    }

    /**
     * Official API theo CRM API Contract:
     * POST /api/auth/login
     *
     * request:
     * - email
     * - password
     *
     * response data:
     * - user
     * - roles
     * - session
     */
    private void handleApiLogin(
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException {

        prepareJson(response);

        String email = request.getParameter("email");
        String password = request.getParameter("password");

        if (isMissing(email) || isMissing(password)) {
            writeJson(
                    response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Vui lòng nhập email và mật khẩu",
                    null
            );
            return;
        }

        AuthService.LoginResult result;

        try {
            result = authService.login(email, password);
        } catch (SQLException e) {
            getServletContext().log(
                    "Login API failed due to database error",
                    e
            );

            writeJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Lỗi hệ thống",
                    null
            );
            return;
        }

        if (result == null) {
            writeJson(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Email hoặc mật khẩu không đúng",
                    null
            );
            return;
        }

        HttpSession session =
                createAuthenticatedSession(request, result);

        Map<String, Object> safeUser = createSafeUser(result);

        Map<String, Object> sessionInfo =
                new LinkedHashMap<>();

        sessionInfo.put(
                "expiresAt",
                resolveExpiresAt(session)
        );

        Map<String, Object> data =
                new LinkedHashMap<>();

        data.put("user", safeUser);
        data.put("roles", result.roles());
        data.put("session", sessionInfo);

        writeJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Đăng nhập thành công",
                data
        );
    }

    private HttpSession createAuthenticatedSession(
            HttpServletRequest request,
            AuthService.LoginResult result) {

        HttpSession oldSession =
                request.getSession(false);

        if (oldSession != null) {
            try {
                oldSession.invalidate();
            } catch (IllegalStateException ignored) {
                // Session đã bị invalidate trước đó.
            }
        }

        HttpSession session =
                request.getSession(true);

        session.setAttribute(
                "userId",
                Long.valueOf(result.userId())
        );

        session.setAttribute(
                SessionKey.ROLES,
                result.roles()
        );

        session.setAttribute(
                SessionKey.CURRENT_USER,
                createSafeUser(result)
        );

        if (result.displayName() != null) {
            session.setAttribute(
                    "displayName",
                    result.displayName()
            );
        }

        return session;
    }

    private Map<String, Object> createSafeUser(
            AuthService.LoginResult result) {

        Map<String, Object> user =
                new LinkedHashMap<>();

        user.put("id", result.userId());
        user.put("displayName", result.displayName());

        return user;
    }

    private String resolveExpiresAt(
            HttpSession session) {

        int maxInactive =
                session.getMaxInactiveInterval();

        if (maxInactive <= 0) {
            return null;
        }

        long expiresAt =
                session.getLastAccessedTime()
                        + ((long) maxInactive * 1000L);

        return Instant.ofEpochMilli(expiresAt)
                .toString();
    }

    private void forwardLogin(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        request.getRequestDispatcher(
                "/jsp/auth/login.jsp"
        ).forward(request, response);
    }

    private boolean isMissing(String value) {
        return value == null || value.isBlank();
    }

    private void prepareJson(
            HttpServletResponse response) {

        response.setContentType("application/json");
        response.setCharacterEncoding(
                StandardCharsets.UTF_8.name()
        );
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data)
            throws IOException {

        prepareJson(response);
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

    private record ApiResponse(
            boolean success,
            String message,
            Object data) {
    }
}