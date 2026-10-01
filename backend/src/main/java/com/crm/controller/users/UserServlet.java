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
        "/users/lock-handover",
        "/users/unlock",
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

        boolean apiRequest = "/api/users".equals(request.getServletPath());
        Long actorUserId = extractActorUserId(request);

        if (actorUserId == null) {
            if (apiRequest) {
                writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                        "Yêu cầu đăng nhập", null);
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
            }
            return;
        }

        if (!hasPermissionAdminRole(request)) {
            if (apiRequest) {
                writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                        "Không có quyền quản lý tài khoản người dùng", null);
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }

        try {
            if ("/users".equals(request.getServletPath())) {
                showList(request, response);
                return;
            }

            if ("/users/detail".equals(request.getServletPath())) {
                showDetail(request, response);
                return;
            }

            if ("/api/users".equals(request.getServletPath())) {
                String pathInfo = request.getPathInfo();

                if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
                    handleApiList(request, response);
                    return;
                }

                Long userId = parseUserIdPath(pathInfo);
                if (userId != null) {
                    handleApiDetail(response, userId);
                    return;
                }
            }

            writeJson(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    false,
                    "Endpoint không tồn tại",
                    null
            );

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to load CRM-28 user data", e);
            writeJson(
                    response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    false,
                    "Không thể tải dữ liệu người dùng lúc này",
                    null
            );
        }
    }
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String servletPath = request.getServletPath();
        if ("/users/lock-handover".equals(servletPath)
                || "/users/unlock".equals(servletPath)) {
            handleViewStatusChange(request, response, servletPath);
            return;
        }

        if (!"/api/users".equals(request.getServletPath())) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Endpoint không tồn tại", null);
            return;
        }

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();

        try {
            if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
                handleCreateUser(request, response);
                return;
            }

            Long directUserId = parseUserIdPath(pathInfo);
            if (directUserId != null) {
                handleUpdateUser(request, response, directUserId);
                return;
            }

            String[] pathParts = splitApiPath(pathInfo);
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

            switch (pathParts[2]) {
                case "lock" -> handleLock(request, response, targetUserId, actorUserId, false);
                case "lock-handover" -> handleLock(
                        request, response, targetUserId, actorUserId, true);
                case "unlock" -> handleUnlock(response, targetUserId);
                case "transfer-data" -> handleTransfer(
                        request, response, targetUserId, actorUserId);
                case "team" -> handleTeamAssignment(request, response, targetUserId);
                default -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Endpoint không tồn tại", null);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to process CRM-28 user operation", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xử lý yêu cầu lúc này", null);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (!"/api/users".equals(request.getServletPath())) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Endpoint không tồn tại", null);
            return;
        }

        if (extractActorUserId(request) == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        Long userId = parseUserIdPath(request.getPathInfo());
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "ID người dùng không hợp lệ", null);
            return;
        }

        try {
            handleUpdateUser(request, response, userId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to update CRM-28 user", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể cập nhật người dùng lúc này", null);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (!"/api/users".equals(request.getServletPath())) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Endpoint không tồn tại", null);
            return;
        }

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        if (!hasPermissionAdminRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền xóa tài khoản người dùng", null);
            return;
        }

        Long userId = parseUserIdPath(request.getPathInfo());
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "ID người dùng không hợp lệ", null);
            return;
        }

        try {
            UserService.DeleteUserStatus result =
                    userService.deleteUser(userId, actorUserId);

            switch (result) {
                case SUCCESS -> writeJson(
                        response, HttpServletResponse.SC_OK, true,
                        "Xóa người dùng thành công",
                        Map.of("userId", userId));
                case NOT_FOUND -> writeJson(
                        response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy người dùng", null);
                case SELF_DELETE -> writeJson(
                        response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "Không thể tự xóa tài khoản đang đăng nhập", null);
                case REFERENCED_DATA -> writeJson(
                        response, HttpServletResponse.SC_CONFLICT, false,
                        "Không thể xóa người dùng vì đang có dữ liệu liên quan", null);
                case DELETE_CONFLICT -> writeJson(
                        response, HttpServletResponse.SC_CONFLICT, false,
                        "Không thể xóa người dùng lúc này", null);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to delete CRM-28 user", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Không thể xóa người dùng lúc này", null);
        }
    }
    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, ServletException, IOException {

        int page = parsePositiveInt(request.getParameter("page"), 1);
        int size = parsePositiveInt(request.getParameter("size"), 20);

        String keyword = request.getParameter("q");
        String role = request.getParameter("role");
        String status = request.getParameter("status");

        UserService.UserPage result =
                userService.searchUsers(keyword, role, status, page, size);

        request.setAttribute("users", result.items());
        request.setAttribute("page", result.page());
        request.setAttribute("size", result.size());
        request.setAttribute("totalItems", result.totalItems());
        request.setAttribute("totalPages", result.totalPages());
        request.setAttribute("q", keyword);
        request.setAttribute("role", role);
        request.setAttribute("status", status);
        request.setAttribute("teams", teamService.findAllTeams());

        request.getRequestDispatcher(USER_LIST_JSP)
                .forward(request, response);
    }

    private void handleApiList(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        int page = parsePositiveInt(request.getParameter("page"), 1);
        int size = parsePositiveInt(request.getParameter("size"), 20);

        UserService.UserPage result = userService.searchUsers(
                request.getParameter("q"),
                request.getParameter("role"),
                request.getParameter("status"),
                page,
                size
        );

        writeJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Lấy danh sách người dùng thành công",
                result
        );
    }

    private void handleApiDetail(HttpServletResponse response, long userId)
            throws SQLException, IOException {

        User user = userService.findById(userId);

        if (user == null) {
            writeJson(
                    response,
                    HttpServletResponse.SC_NOT_FOUND,
                    false,
                    "Không tìm thấy người dùng",
                    null
            );
            return;
        }

        writeJson(
                response,
                HttpServletResponse.SC_OK,
                true,
                "Lấy thông tin người dùng thành công",
                user
        );
    }

    private void handleCreateUser(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {

        if (!hasPermissionAdminRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền tạo tài khoản người dùng", null);
            return;
        }

        UserMutationRequest body = readUserMutationRequest(request);

        if (body == null || body.invalidTeamId) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dữ liệu người dùng không hợp lệ", null);
            return;
        }

        UserService.CreateUserResult result = userService.createUser(
                body.username,
                body.email,
                body.fullName,
                body.phone,
                body.teamId
        );

        switch (result.status()) {
            case SUCCESS -> {
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("userId", result.userId());
                data.put("activationEmailRequested", true);

                writeJson(response, HttpServletResponse.SC_CREATED, true,
                        "Tạo tài khoản thành công", data);
            }
            case DUPLICATE_EMAIL -> writeJson(
                    response, HttpServletResponse.SC_CONFLICT, false,
                    "Email đã tồn tại trong hệ thống", null);
            case DUPLICATE_USERNAME -> writeJson(
                    response, HttpServletResponse.SC_CONFLICT, false,
                    "Tên đăng nhập đã tồn tại trong hệ thống", null);
            case INVALID_EMAIL -> writeJson(
                    response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Email không hợp lệ", null);
            case INVALID_INPUT -> writeJson(
                    response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Vui lòng nhập đầy đủ email và họ tên", null);
        }
    }

    private void handleUpdateUser(HttpServletRequest request,
                                  HttpServletResponse response,
                                  long userId)
            throws SQLException, IOException {

        if (!hasPermissionAdminRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền cập nhật tài khoản người dùng", null);
            return;
        }

        UserMutationRequest body = readUserMutationRequest(request);

        if (body == null || body.invalidTeamId) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dữ liệu người dùng không hợp lệ", null);
            return;
        }

        UserService.UpdateUserStatus result = userService.updateUser(
                userId,
                body.username,
                body.email,
                body.fullName,
                body.phone,
                body.teamId
        );

        switch (result) {
            case SUCCESS -> writeJson(
                    response, HttpServletResponse.SC_OK, true,
                    "Cập nhật người dùng thành công",
                    userService.findById(userId));
            case DUPLICATE_EMAIL -> writeJson(
                    response, HttpServletResponse.SC_CONFLICT, false,
                    "Email đã tồn tại trong hệ thống", null);
            case DUPLICATE_USERNAME -> writeJson(
                    response, HttpServletResponse.SC_CONFLICT, false,
                    "Tên đăng nhập đã tồn tại trong hệ thống", null);
            case INVALID_EMAIL -> writeJson(
                    response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Email không hợp lệ", null);
            case INVALID_INPUT -> writeJson(
                    response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dữ liệu người dùng không hợp lệ", null);
            case NOT_FOUND -> writeJson(
                    response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy người dùng", null);
            case UPDATE_CONFLICT -> writeJson(
                    response, HttpServletResponse.SC_CONFLICT, false,
                    "Không thể cập nhật người dùng", null);
        }
    }

    private UserMutationRequest readUserMutationRequest(HttpServletRequest request)
            throws IOException {

        String contentType = request.getContentType();

        if (contentType != null
                && contentType.toLowerCase(java.util.Locale.ROOT)
                        .startsWith("application/json")) {

            try {
                return GSON.fromJson(
                        request.getReader(),
                        UserMutationRequest.class
                );
            } catch (JsonSyntaxException e) {
                return null;
            }
        }

        UserMutationRequest body = new UserMutationRequest();
        body.username = request.getParameter("username");
        body.email = request.getParameter("email");
        body.fullName = request.getParameter("fullName");
        body.phone = request.getParameter("phone");

        String teamId = request.getParameter("teamId");

        if (teamId != null && !teamId.isBlank()) {
            Long parsed = parsePositiveLong(teamId);

            if (parsed == null) {
                body.invalidTeamId = true;
            } else {
                body.teamId = parsed;
            }
        }

        return body;
    }

    private Long parseUserIdPath(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank()) {
            return null;
        }

        String path = pathInfo.startsWith("/")
                ? pathInfo.substring(1)
                : pathInfo;

        if (path.isBlank() || path.contains("/")) {
            return null;
        }

        return parsePositiveLong(path);
    }

    private int parsePositiveInt(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            int parsed = Integer.parseInt(value);
            return parsed > 0 ? parsed : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
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
                                long sourceUserId, long actorUserId) throws SQLException, IOException {
        Long recipientUserId = parsePositiveLong(request.getParameter("toUserId"));
        if (recipientUserId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "toUserId không hợp lệ", null);
            return;
        }

        TransferValidationResult result = userService.validateTransfer(
                sourceUserId, recipientUserId, actorUserId);
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

        loadDetail(request, response, userId);
    }

    private void loadDetail(HttpServletRequest request, HttpServletResponse response, long userId)
            throws SQLException, ServletException, IOException {
        User user = userService.findById(userId);
        if (user == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        request.setAttribute("user", user);
        request.setAttribute("availableRecipients", userService.findAvailableRecipients(userId));
        if ("1".equals(request.getParameter("locked"))) {
            request.setAttribute("message",
                    "Tài khoản đã được khóa và dữ liệu đã được bàn giao.");
        } else if ("1".equals(request.getParameter("unlocked"))) {
            request.setAttribute("message", "Tài khoản đã được mở khóa và có thể đăng nhập lại.");
        }
        request.getRequestDispatcher(USER_DETAIL_JSP).forward(request, response);
    }

    private void handleViewStatusChange(HttpServletRequest request, HttpServletResponse response,
                                        String servletPath)
            throws ServletException, IOException {
        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            response.sendRedirect(request.getContextPath() + "/login?expired=1");
            return;
        }
        if (!hasPermissionAdminRole(request)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Long targetUserId = parsePositiveLong(request.getParameter("userId"));
        if (targetUserId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        try {
            StatusChangeResult result;
            if ("/users/unlock".equals(servletPath)) {
                result = userService.unlockUser(targetUserId);
                if (result == StatusChangeResult.SUCCESS) {
                    response.sendRedirect(request.getContextPath()
                            + "/users/detail?id=" + targetUserId + "&unlocked=1");
                    return;
                }
            } else {
                if (!isConfirmed(request.getParameter("confirm"))) {
                    request.setAttribute("error", "Vui lòng xác nhận thao tác khóa và bàn giao.");
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    loadDetail(request, response, targetUserId);
                    return;
                }

                Long recipientUserId = null;
                String recipientValue = request.getParameter("recipientId");
                if (recipientValue != null && !recipientValue.isBlank()) {
                    recipientUserId = parsePositiveLong(recipientValue);
                    if (recipientUserId == null) {
                        request.setAttribute("error", "Người tiếp nhận không hợp lệ.");
                        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                        loadDetail(request, response, targetUserId);
                        return;
                    }
                }

                result = userService.lockUser(targetUserId, actorUserId, recipientUserId,
                        request.getParameter("reason"));
                if (result == StatusChangeResult.SUCCESS) {
                    response.sendRedirect(request.getContextPath()
                            + "/users/detail?id=" + targetUserId + "&locked=1");
                    return;
                }
            }

            request.setAttribute("error", statusChangeMessage(result));
            response.setStatus(result == StatusChangeResult.TARGET_NOT_FOUND
                    || result == StatusChangeResult.RECIPIENT_NOT_FOUND
                    ? HttpServletResponse.SC_NOT_FOUND
                    : HttpServletResponse.SC_CONFLICT);
            loadDetail(request, response, targetUserId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Unable to process user status change view", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private String statusChangeMessage(StatusChangeResult result) {
        return switch (result) {
            case INVALID_REASON -> "Lý do khóa là bắt buộc và không được vượt quá 500 ký tự.";
            case SELF_LOCK -> "Bạn không thể tự khóa tài khoản đang đăng nhập.";
            case TARGET_NOT_FOUND -> "Không tìm thấy tài khoản cần xử lý.";
            case INVALID_CURRENT_STATUS -> "Trạng thái tài khoản đã thay đổi. Vui lòng tải lại trang.";
            case RECIPIENT_REQUIRED -> "Tài khoản đang phụ trách khách hàng hoặc cơ hội. Vui lòng chọn người tiếp nhận.";
            case SAME_USER -> "Người tiếp nhận không được trùng với tài khoản bị khóa.";
            case RECIPIENT_NOT_FOUND -> "Không tìm thấy người tiếp nhận đã chọn.";
            case RECIPIENT_NOT_ACTIVE -> "Người tiếp nhận phải là tài khoản đang hoạt động.";
            case UPDATE_CONFLICT -> "Không thể cập nhật trạng thái tài khoản do có thay đổi đồng thời.";
            case TRANSFER_INCOMPLETE -> "Bàn giao chưa hoàn tất. Toàn bộ thao tác khóa đã được hoàn tác.";
            case SUCCESS -> "Thao tác hoàn tất.";
        };
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

    private static final class UserMutationRequest {
        private String username;
        private String email;
        private String fullName;
        private String phone;
        private Long teamId;
        private boolean invalidTeamId;
    }
    private static final class TeamAssignmentRequest {
        private Long teamId;
    }

    private record TeamAssignmentData(long userId, long teamId) {
    }
    private record ApiResponse(boolean success, String message, Object data) {
    }

}
