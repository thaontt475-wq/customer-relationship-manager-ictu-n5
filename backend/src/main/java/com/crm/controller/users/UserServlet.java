package com.crm.controller.users;

import com.crm.model.User;
import com.crm.service.users.UserService;
import com.crm.service.teams.TeamService;
import com.crm.service.teams.TeamService.AssignmentResult;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.crm.service.users.UserService.LockHandoverResult;
import com.crm.util.SessionKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({
        "/users",
        "/users/detail",
        "/api/users/*"
})
public class UserServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UserServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final String USER_LIST_JSP = "/jsp/users/user-list.jsp";
    private static final String USER_DETAIL_JSP = "/jsp/users/user-detail.jsp";

    private final UserService userService = new UserService();
    private final TeamService teamService = new TeamService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        try {
            if ("/users".equals(request.getServletPath())) {
                request.setAttribute("users", userService.findAll());
                request.getRequestDispatcher(USER_LIST_JSP).forward(request, response);
                return;
            }
            if ("/users/detail".equals(request.getServletPath())) {
                showDetail(request, response);
                return;
            }
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load user data", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        if (!"/api/users".equals(request.getServletPath())) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long teamTargetUserId = parseTeamAssignmentPath(request.getPathInfo());
        if (teamTargetUserId != null) {
            handleTeamAssignment(request, response, teamTargetUserId);
            return;
        }
        Long targetUserId = parseLockHandoverPath(request.getPathInfo());
        if (targetUserId == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        if (actorUserId.equals(targetUserId)) {
            forwardPostError(request, response, targetUserId,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Không thể tự khóa tài khoản đang đăng nhập.");
            return;
        }

        Long recipientUserId = parsePositiveLong(request.getParameter("recipientId"));
        if (recipientUserId == null) {
            forwardPostError(request, response, targetUserId,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Người nhận bàn giao không hợp lệ.");
            return;
        }

        String confirmLock = request.getParameter("confirmLock");
        if (!("on".equalsIgnoreCase(confirmLock) || "true".equalsIgnoreCase(confirmLock))) {
            forwardPostError(request, response, targetUserId,
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Bạn phải xác nhận thao tác khóa tài khoản.");
            return;
        }

        String lockReason = request.getParameter("lockReason");
        try {
            LockHandoverResult result = userService.lockAndHandover(
                    targetUserId, recipientUserId, actorUserId, lockReason);
            handleLockResult(request, response, targetUserId, result);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to lock user and record handover", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void handleTeamAssignment(
            HttpServletRequest request,
            HttpServletResponse response,
            long userId) throws IOException {

        if (extractActorUserId(request) == null) {
            writeJson(response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    false,
                    "Yêu cầu đăng nhập",
                    null);
            return;
        }

        if (!hasPermissionAdminRole(request)) {
            writeJson(response,
                    HttpServletResponse.SC_FORBIDDEN,
                    false,
                    "Không có quyền gán nhóm kinh doanh",
                    null);
            return;
        }

        TeamAssignmentRequest body;

        try {
            body = GSON.fromJson(
                    request.getReader(),
                    TeamAssignmentRequest.class
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
                || body.teamId == null
                || body.teamId <= 0) {

            writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST,
                    false,
                    "Team ID không hợp lệ",
                    null);
            return;
        }

        try {
            AssignmentResult result =
                    teamService.assignUserToTeam(
                            userId,
                            body.teamId
                    );

            switch (result) {
                case SUCCESS -> writeJson(
                        response,
                        HttpServletResponse.SC_OK,
                        true,
                        "Gán nhóm kinh doanh thành công",
                        new TeamAssignmentData(
                                userId,
                                body.teamId
                        )
                );

                case INVALID_USER,
                     INVALID_TEAM -> writeJson(
                        response,
                        HttpServletResponse.SC_BAD_REQUEST,
                        false,
                        "Dữ liệu gán nhóm không hợp lệ",
                        null
                );

                case USER_NOT_FOUND -> writeJson(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        false,
                        "Không tìm thấy người dùng",
                        null
                );

                case TEAM_NOT_FOUND -> writeJson(
                        response,
                        HttpServletResponse.SC_NOT_FOUND,
                        false,
                        "Không tìm thấy nhóm kinh doanh",
                        null
                );

                case UPDATE_CONFLICT -> writeJson(
                        response,
                        HttpServletResponse.SC_CONFLICT,
                        false,
                        "Không thể cập nhật nhóm kinh doanh",
                        null
                );
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE,
                    "Unable to assign user to team", e);

            writeJson(response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Lỗi hệ thống khi gán nhóm",
                    null);
        }
    }
    private void showDetail(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {
        Long userId = parsePositiveLong(request.getParameter("id"));
        if (userId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        User user = userService.findById(userId);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("user", user);
        request.setAttribute("availableRecipients", userService.findAvailableRecipients(userId));
        if ("1".equals(request.getParameter("locked"))) {
            request.setAttribute("message",
                    "Khóa tài khoản thành công. Thông tin người nhận bàn giao đã được ghi nhận.");
        }
        request.getRequestDispatcher(USER_DETAIL_JSP).forward(request, response);
    }

    private void handleLockResult(HttpServletRequest request, HttpServletResponse response,
                                  long targetUserId, LockHandoverResult result)
            throws SQLException, ServletException, IOException {
        switch (result) {
            case SUCCESS -> response.sendRedirect(request.getContextPath()
                    + "/users/detail?id=" + targetUserId + "&locked=1");
            case TARGET_NOT_FOUND -> response.sendError(HttpServletResponse.SC_NOT_FOUND);
            case TARGET_NOT_ACTIVE, UPDATE_CONFLICT -> forwardPostError(
                    request, response, targetUserId, HttpServletResponse.SC_CONFLICT,
                    "Tài khoản mục tiêu không còn ở trạng thái ACTIVE.");
            case RECIPIENT_NOT_FOUND -> forwardPostError(
                    request, response, targetUserId, HttpServletResponse.SC_BAD_REQUEST,
                    "Không tìm thấy người nhận bàn giao.");
            case RECIPIENT_NOT_ACTIVE -> forwardPostError(
                    request, response, targetUserId, HttpServletResponse.SC_BAD_REQUEST,
                    "Người nhận bàn giao phải là tài khoản ACTIVE.");
            case SAME_RECIPIENT -> forwardPostError(
                    request, response, targetUserId, HttpServletResponse.SC_BAD_REQUEST,
                    "Không thể chọn chính tài khoản bị khóa làm người nhận bàn giao.");
            case SELF_LOCK -> forwardPostError(
                    request, response, targetUserId, HttpServletResponse.SC_BAD_REQUEST,
                    "Không thể tự khóa tài khoản đang đăng nhập.");
            case INVALID_REASON -> forwardPostError(
                    request, response, targetUserId, HttpServletResponse.SC_BAD_REQUEST,
                    "Lý do khóa là bắt buộc và không được vượt quá 500 ký tự.");
        }
    }

    private void forwardPostError(HttpServletRequest request, HttpServletResponse response,
                                  long targetUserId, int status, String message)
            throws ServletException, IOException {
        try {
            User user = userService.findById(targetUserId);
            if (user == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            request.setAttribute("user", user);
            request.setAttribute("availableRecipients", userService.findAvailableRecipients(targetUserId));
            request.setAttribute("error", message);
            response.setStatus(status);
            request.getRequestDispatcher(USER_DETAIL_JSP).forward(request, response);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to reload user detail after validation error", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
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

        Object currentUser;
        try {
            currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        } catch (IllegalStateException e) {
            return null;
        }

        if (currentUser instanceof User user) {
            return user.getId() > 0 ? user.getId() : null;
        }
        if (currentUser instanceof Map<?, ?> map) {
            return parseIdValue(map.get("id"));
        }
        return parseIdValue(currentUser);
    }

    private Long parseIdValue(Object value) {
        if (value instanceof Number number) {
            long id = number.longValue();
            return id > 0 ? id : null;
        }
        return value instanceof String text ? parsePositiveLong(text) : null;
    }

    private Long parseTeamAssignmentPath(String pathInfo) {
        if (pathInfo == null) {
            return null;
        }

        String[] parts = pathInfo.split("/", -1);
        if (parts.length != 3 || !"team".equals(parts[2])) {
            return null;
        }

        return parsePositiveLong(parts[1]);
    }
    private Long parseLockHandoverPath(String pathInfo) {
        if (pathInfo == null) {
            return null;
        }
        String[] parts = pathInfo.split("/", -1);
        if (parts.length != 3 || !"lock-handover".equals(parts[2])) {
            return null;
        }
        return parsePositiveLong(parts[1]);
    }

    private boolean hasPermissionAdminRole(HttpServletRequest request) {
        HttpSession session;

        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return false;
        }

        if (session == null) {
            return false;
        }

        Object rolesValue;

        try {
            rolesValue = session.getAttribute(SessionKey.ROLES);
        } catch (IllegalStateException e) {
            return false;
        }

        if (!(rolesValue instanceof java.util.Collection<?> roles)) {
            return false;
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.trim().toLowerCase(java.util.Locale.ROOT))
                .anyMatch(role ->
                        "admin".equals(role)
                                || "director".equals(role));
    }
    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
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

    private static final class TeamAssignmentRequest {
        private Long teamId;
    }

    private record TeamAssignmentData(
            long userId,
            long teamId) {
    }

    private record ApiResponse(
            boolean success,
            String message,
            Object data) {
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
