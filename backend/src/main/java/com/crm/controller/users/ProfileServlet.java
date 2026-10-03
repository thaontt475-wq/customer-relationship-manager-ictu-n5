package com.crm.controller.users;

import com.crm.model.User;
import com.crm.controller.ServerForms;
import com.crm.service.users.ProfileService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling user profile operations for CRM-35:
 * - GET  /profile              — Render profile JSP view
 * - GET  /api/profile          — Get current user profile as JSON
 * - POST /api/profile          — Update current user profile (fullName, phone, signature)
 * - PUT  /api/profile          — Update current user profile (fullName, phone, signature)
 * - POST /profile              — Form-based update profile
 */
@WebServlet({
        "/profile",
        "/user/profile",
        "/api/profile",
        "/api/user/profile",
        "/api/users/profile",
        "/api/users/me"
})
public class ProfileServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ProfileServlet.class.getName());
    private static final String PROFILE_JSP = "/jsp/users/profile.jsp";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final ProfileService profileService;

    public ProfileServlet() {
        this.profileService = new ProfileService();
    }

    public ProfileServlet(ProfileService profileService) {
        this.profileService = profileService != null ? profileService : new ProfileService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        boolean isApi = isApiRequest(request);

        if (actorUserId == null) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
            }
            return;
        }

        try {
            User user = profileService.getUserProfile(actorUserId);
            if (user == null) {
                if (isApi) {
                    writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Không tìm thấy người dùng", null);
                } else {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                }
                return;
            }

            if (isApi) {
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy thông tin hồ sơ thành công", new ProfileResponseData(user));
            } else {
                request.setAttribute("profileUser", user);
                request.setAttribute("signature", user.getSignature() != null ? user.getSignature() : "");
                request.getRequestDispatcher(PROFILE_JSP).forward(request, response);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-35: Error retrieving user profile", e);
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                        "Lỗi hệ thống khi tải thông tin hồ sơ", null);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleProfileUpdate(request, response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        handleProfileUpdate(request, response);
    }

    // === Update handling logic ===

    private void handleProfileUpdate(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        boolean isApi = isApiRequest(request);

        if (actorUserId == null) {
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
            }
            return;
        }

        // Browser HTML form: CSRF validation. Preserve the existing JSON API contract.
        if (!isApi && !ServerForms.checkCsrf(request, response)) {
            return;
        }

        // Extract input fields
        String fullName = null;
        String phone = null;
        String signature = null;

        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                ProfileUpdateRequest body = GSON.fromJson(request.getReader(), ProfileUpdateRequest.class);
                if (body != null) {
                    fullName = body.fullName();
                    phone = body.phone();
                    signature = body.signature();
                    // Any email, team, or role provided in JSON is deliberately IGNORED
                }
            } catch (JsonSyntaxException e) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "Dữ liệu JSON không hợp lệ", null);
                return;
            }
        } else {
            // Form submission
            fullName = request.getParameter("fullName");
            phone = request.getParameter("phone");
            signature = request.getParameter("signature");
        }

        try {
            // Business logic validation and update
            // RULE 1: Only fullName, phone, signature are updated.
            // RULE 2: Email, team, role cannot be edited and are preserved in DB.
            // RULE 3: Phone is validated using Vietnamese phone format regex.
            User updatedUser = profileService.updateProfile(actorUserId, fullName, phone, signature);

            // Update session cache with updated user profile
            HttpSession session = request.getSession(false);
            if (session != null && updatedUser != null) {
                session.setAttribute(SessionKey.CURRENT_USER, updatedUser);
                if (updatedUser.getFullName() != null) {
                    session.setAttribute("displayName", updatedUser.getFullName());
                }
            }

            if (isApi) {
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Cập nhật hồ sơ cá nhân thành công.", new ProfileResponseData(updatedUser));
            } else {
                ServerForms.setToast(request, "success", "Cập nhật thành công", "Thông tin hồ sơ cá nhân đã được lưu.");
                response.sendRedirect(request.getContextPath() + "/profile?updated=1");
            }

        } catch (IllegalArgumentException e) {
            // Validation error (e.g. invalid phone number, empty name)
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
            } else {
                request.setAttribute("error", e.getMessage());
                try {
                    User user = profileService.getUserProfile(actorUserId);
                    request.setAttribute("profileUser", user);
                    request.setAttribute("signature", signature != null ? signature : "");
                } catch (SQLException ignored) {}
                request.getRequestDispatcher(PROFILE_JSP).forward(request, response);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-35: Database error updating user profile", e);
            if (isApi) {
                writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                        "Lỗi hệ thống khi cập nhật hồ sơ cá nhân", null);
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        }
    }

    // === Helpers ===

    private boolean isApiRequest(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String acceptHeader = request.getHeader("Accept");
        String contentType = request.getContentType();

        return (servletPath != null && servletPath.startsWith("/api/"))
                || (acceptHeader != null && acceptHeader.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }

    private Long extractActorUserId(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return null;
        }
        if (session == null) {
            return null;
        }
        try {
            Object directUserId = session.getAttribute("userId");
            Long parsedDirect = parseIdValue(directUserId);
            if (parsedDirect != null) {
                return parsedDirect;
            }
            Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
            if (currentUser instanceof User u && u.getId() > 0) {
                return u.getId();
            }
            if (currentUser instanceof Map<?, ?> map) {
                return parseIdValue(map.get("id"));
            }
            return parseIdValue(currentUser);
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private Long parseIdValue(Object value) {
        if (value instanceof Number number) {
            long id = number.longValue();
            return id > 0 ? id : null;
        }
        if (value instanceof String text) {
            try {
                long parsed = Long.parseLong(text);
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    // === Request / Response DTOs ===

    private record ProfileUpdateRequest(String fullName, String phone, String signature,
                                        String email, Long teamId, String role) { }

    private record ProfileResponseData(
            long id,
            String username,
            String email,
            String fullName,
            String phone,
            String signature,
            String teamName,
            java.util.List<String> roles
    ) {
        public ProfileResponseData(User user) {
            this(
                    user != null ? user.getId() : 0,
                    user != null ? user.getUsername() : "",
                    user != null ? user.getEmail() : "",
                    user != null ? user.getFullName() : "",
                    user != null ? user.getPhone() : "",
                    user != null ? user.getSignature() : "",
                    user != null ? user.getTeamName() : "",
                    user != null && user.getRoles() != null
                            ? user.getRoles().stream().map(com.crm.model.Role::getName).toList()
                            : java.util.List.of()
            );
        }
    }

    private record ApiResponse(boolean success, String message, Object data) { }
}
