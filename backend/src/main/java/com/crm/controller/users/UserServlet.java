package com.crm.controller.users;

import com.crm.model.User;
import com.crm.service.teams.TeamService;
import com.crm.service.teams.TeamService.AssignmentResult;
import com.crm.service.users.UserService;
import com.crm.service.users.UserService.StatusChangeResult;
import com.crm.service.users.UserService.TransferValidationResult;
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
import java.util.LinkedHashMap;
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
    private static final String USER_LIST_JSP = "/jsp/users/user-list.jsp";
    private static final String USER_DETAIL_JSP = "/jsp/users/user-detail.jsp";
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final UserService userService = new UserService();
    private final TeamService teamService = new TeamService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
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
            writeJson(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false,
                    "Phương thức không được hỗ trợ", null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load user data", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (!"/api/users".equals(request.getServletPath())) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Endpoint không tồn tại", null);
            return;
        }

        String[] pathParts = splitApiPath(request.getPathInfo());
        if (pathParts == null || !isSupportedAction(pathParts[2])) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Endpoint không tồn tại", null);
            return;
        }

        Long targetUserId = parsePositiveLong(pathParts[1]);
        if (targetUserId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "ID người dùng không hợp lệ", null);
            return;
        }

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        try {
            switch (pathParts[2]) {
                case "lock" -> handleLock(request, response, targetUserId, actorUserId, false);
                case "lock-handover" -> handleLock(
                        request, response, targetUserId, actorUserId, true);
                case "unlock" -> handleUnlock(response, targetUserId);
                case "transfer-data" -> handleTransfer(request, response, targetUserId);
                case "team" -> handleTeamAssignment(request, response, targetUserId);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Endpoint không tồn tại", null);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to process CRM-30 user operation", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý yêu cầu lúc này", null);
        }
    }

    private void handleTeamAssignment(HttpServletRequest request, HttpServletResponse response,
                                      long userId) throws SQLException, IOException {
        if (!hasPermissionAdminRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền gán nhóm kinh doanh", null);
            return;
        }

        TeamAssignmentRequest body;
        try {
            body = GSON.fromJson(request.getReader(), TeamAssignmentRequest.class);
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "JSON không hợp lệ", null);
            return;
        }

        if (body == null || body.teamId == null || body.teamId <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Team ID không hợp lệ", null);
            return;
        }

        AssignmentResult result = teamService.assignUserToTeam(userId, body.teamId);
        switch (result) {
            case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                    "Gán nhóm kinh doanh thành công",
                    new TeamAssignmentData(userId, body.teamId));
            case INVALID_USER, INVALID_TEAM -> writeJson(
                    response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dữ liệu gán nhóm không hợp lệ", null);
            case USER_NOT_FOUND -> writeJson(
                    response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy người dùng", null);
            case TEAM_NOT_FOUND -> writeJson(
                    response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy nhóm kinh doanh", null);
            case UPDATE_CONFLICT -> writeJson(
                    response, HttpServletResponse.SC_CONFLICT, false,
                    "Không thể cập nhật nhóm kinh doanh", null);
        }
    }
    private void handleLock(HttpServletRequest request, HttpServletResponse response,
                            long targetUserId, long actorUserId, boolean requireConfirmation)
            throws SQLException, IOException {
        if (requireConfirmation && !isConfirmed(request.getParameter("confirm"))) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Cần xác nhận thao tác khóa và bàn giao", null);
            return;
        }

        String recipientValue = request.getParameter("recipientId");
        Long recipientUserId = null;
        if (recipientValue != null && !recipientValue.isBlank()) {
            recipientUserId = parsePositiveLong(recipientValue);
            if (recipientUserId == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "recipientId không hợp lệ", null);
                return;
            }
        }

        String reason = request.getParameter("reason");
        if (reason == null) {
            reason = request.getParameter("lockReason");
        }
        StatusChangeResult result = userService.lockUser(
                targetUserId, actorUserId, recipientUserId, reason);
        switch (result) {
            case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                    "Khóa tài khoản thành công", statusData(targetUserId, "LOCKED"));
            case INVALID_REASON -> writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Lý do khóa là bắt buộc và không được vượt quá 500 ký tự", null);
            case SELF_LOCK -> writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Không thể tự khóa tài khoản đang đăng nhập", null);
            case TARGET_NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài khoản mục tiêu", null);
            case INVALID_CURRENT_STATUS -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Chỉ tài khoản ACTIVE mới có thể bị khóa", null);
            case RECIPIENT_REQUIRED -> writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Tài khoản đang sở hữu customer hoặc opportunity; recipientId là bắt buộc", null);
            case SAME_USER -> writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Tài khoản nhận không được trùng tài khoản bị khóa", null);
            case RECIPIENT_NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài khoản nhận", null);
            case RECIPIENT_NOT_ACTIVE -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Tài khoản nhận phải ở trạng thái ACTIVE", null);
            case UPDATE_CONFLICT -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Trạng thái tài khoản đã thay đổi, vui lòng thử lại", null);
            case TRANSFER_INCOMPLETE -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Bàn giao ownership chưa hoàn tất; thao tác khóa đã được rollback", null);
        }
    }

    private void handleUnlock(HttpServletResponse response, long targetUserId)
            throws SQLException, IOException {
        StatusChangeResult result = userService.unlockUser(targetUserId);
        switch (result) {
            case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                    "Mở khóa tài khoản thành công", statusData(targetUserId, "ACTIVE"));
            case TARGET_NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài khoản mục tiêu", null);
            case INVALID_CURRENT_STATUS -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Chỉ tài khoản LOCKED mới có thể được mở khóa", null);
            case UPDATE_CONFLICT -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Trạng thái tài khoản đã thay đổi, vui lòng thử lại", null);
            case INVALID_REASON, SELF_LOCK, RECIPIENT_REQUIRED, SAME_USER,
                 RECIPIENT_NOT_FOUND, RECIPIENT_NOT_ACTIVE, TRANSFER_INCOMPLETE -> writeJson(
                    response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý yêu cầu lúc này", null);
        }
    }

    private void handleTransfer(HttpServletRequest request, HttpServletResponse response,
                                long sourceUserId) throws SQLException, IOException {
        Long recipientUserId = parsePositiveLong(request.getParameter("toUserId"));
        if (recipientUserId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "toUserId không hợp lệ", null);
            return;
        }

        TransferValidationResult result = userService.validateTransfer(sourceUserId, recipientUserId);
        switch (result) {
            case SAME_USER -> writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Tài khoản nguồn và tài khoản nhận không được trùng nhau", null);
            case SOURCE_NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài khoản nguồn", null);
            case RECIPIENT_NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài khoản nhận", null);
            case RECIPIENT_NOT_ACTIVE -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Tài khoản nhận phải ở trạng thái ACTIVE", null);
            case NOT_SUPPORTED -> writeJson(response, HttpServletResponse.SC_NOT_IMPLEMENTED, false,
                    "Chưa thể chuyển dữ liệu vì schema hiện tại chưa có bảng dữ liệu sở hữu nghiệp vụ",
                    transferUnavailableData(sourceUserId, recipientUserId));
            case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                    "Bàn giao ownership thành công",
                    transferResultData(sourceUserId, recipientUserId));
            case TRANSFER_INCOMPLETE -> writeJson(response, HttpServletResponse.SC_CONFLICT, false,
                    "Bàn giao ownership chưa hoàn tất; giao dịch đã được rollback", null);
        }
    }

    private Map<String, Object> statusData(long userId, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", userId);
        data.put("status", status);
        return data;
    }

    private Map<String, Object> transferUnavailableData(long sourceUserId, long recipientUserId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sourceUserId", sourceUserId);
        data.put("toUserId", recipientUserId);
        data.put("status", "NOT_SUPPORTED");
        return data;
    }

    private Map<String, Object> transferResultData(long sourceUserId, long recipientUserId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("sourceUserId", sourceUserId);
        data.put("recipientId", recipientUserId);
        data.put("status", "SUCCESS");
        return data;
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

        Object directUserId;
        try {
            directUserId = session.getAttribute("userId");
        } catch (IllegalStateException e) {
            return null;
        }

        Long parsedDirectUserId = parseIdValue(directUserId);
        if (parsedDirectUserId != null) {
            return parsedDirectUserId;
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

    private String[] splitApiPath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return null;
        }
        String[] parts = pathInfo.split("/", -1);
        return parts.length == 3 && !parts[1].isBlank() && !parts[2].isBlank()
                ? parts : null;
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
                .anyMatch(role -> "admin".equals(role) || "director".equals(role));
    }
    private boolean isSupportedAction(String action) {
        return "lock".equals(action) || "lock-handover".equals(action)
                || "unlock".equals(action) || "transfer-data".equals(action)
                || "team".equals(action);
    }

    private boolean isConfirmed(String value) {
        return value != null && ("true".equalsIgnoreCase(value)
                || "on".equalsIgnoreCase(value)
                || "yes".equalsIgnoreCase(value)
                || "1".equals(value));
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

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    private static final class TeamAssignmentRequest {
        private Long teamId;
    }

    private record TeamAssignmentData(long userId, long teamId) {
    }
    private record ApiResponse(boolean success, String message, Object data) {
    }

}
