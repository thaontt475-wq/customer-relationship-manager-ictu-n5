package com.crm.controller.permissions;

import com.crm.model.Role;
import com.crm.model.User;
import com.crm.service.permissions.UserRoleService;
import com.crm.service.permissions.UserRoleService.RoleAssignmentResult;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet exposing the role and team assignment API for CRM-29.
 * Architecture: Filter -> Servlet -> UserRoleService -> UserRoleDAO / UserTeamDAO -> JDBC -> MySQL 8.0.
 *
 * Endpoints:
 * GET  /api/roles                      — list all roles
 * GET  /api/roles/all                  — list all roles
 * GET  /api/roles/user/{id}            — list roles of a specific user
 * GET  /api/roles/user/{id}/team       — get team ID of a specific user
 * POST /api/roles/assign               — assign roles and/or team to a user (multi-role with business rules)
 * POST /api/roles/team                 — assign or remove a user's team
 * POST /api/roles/user/{id}/team       — assign or remove a user's team
 * PUT  /api/roles/user/{id}/add        — add a single role to a user
 * DELETE /api/roles/user/{id}/remove   — remove a single role from a user
 */
@WebServlet({"/api/roles", "/api/roles/*"})
public class UserRoleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UserRoleServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final UserRoleService userRoleService;

    public UserRoleServlet() {
        this.userRoleService = new UserRoleService();
    }

    public UserRoleServlet(UserRoleService userRoleService) {
        this.userRoleService = userRoleService != null ? userRoleService : new UserRoleService();
    }

    /**
     * GET /api/roles
     * GET /api/roles/all
     * GET /api/roles/user/{id}
     * GET /api/roles/user/{id}/team
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (!isAuthenticated(request)) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        String pathInfo = request.getPathInfo();

        try {
            // GET /api/roles or GET /api/roles/ or GET /api/roles/all
            if (pathInfo == null || "/".equals(pathInfo) || "/all".equals(pathInfo)) {
                List<Role> roles = userRoleService.findAllRoles();
                writeJson(response, HttpServletResponse.SC_OK, true, "Lấy danh sách vai trò thành công", roles);
                return;
            }

            // GET /api/roles/user/{id}/team
            if (pathInfo.startsWith("/user/") && pathInfo.endsWith("/team")) {
                String sub = pathInfo.substring("/user/".length(), pathInfo.length() - "/team".length());
                Long userId = parsePositiveId(sub);
                if (userId != null) {
                    Long teamId = userRoleService.findTeamIdByUserId(userId);
                    writeJson(response, HttpServletResponse.SC_OK, true,
                            "Lấy thông tin nhóm kinh doanh thành công", new UserTeamData(userId, teamId));
                    return;
                }
            }

            // GET /api/roles/user/{id}
            Long userId = parseUserPath(pathInfo, "/user/");
            if (userId != null) {
                List<Role> roles = userRoleService.findRolesByUserId(userId);
                Long teamId = userRoleService.findTeamIdByUserId(userId);
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy vai trò người dùng thành công", new UserRolesAndTeamData(userId, roles, teamId));
                return;
            }

            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Endpoint không tồn tại", null);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-29: Error retrieving roles/teams", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi lấy danh sách vai trò", null);
        }
    }

    /**
     * POST /api/roles/assign — assign multiple roles and optionally team to a user.
     * POST /api/roles/team   — assign or remove a user's team.
     * POST /api/roles/user/{id}/team — assign or remove a user's team by URL path.
     *
     * Business rules enforced:
     * 1. Multi-role (RULE 1): accepts multiple role IDs simultaneously.
     * 2. Team Lead validation (RULE 2): Team Lead role requires user to belong to a team (team_id not null).
     * 3. Self-revoke Admin guard (RULE 3): admin cannot remove their own Admin role.
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        if (!hasAdminOrDirectorRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền phân vai trò người dùng", null);
            return;
        }

        String pathInfo = request.getPathInfo();

        // 1. POST /api/roles/assign
        if ("/assign".equals(pathInfo)) {
            handleAssignRolesAndTeam(request, response, actorUserId);
            return;
        }

        // 2. POST /api/roles/team
        if ("/team".equals(pathInfo)) {
            handleAssignTeam(request, response, actorUserId, null);
            return;
        }

        // 3. POST /api/roles/user/{id}/team
        if (pathInfo != null && pathInfo.startsWith("/user/") && pathInfo.endsWith("/team")) {
            String sub = pathInfo.substring("/user/".length(), pathInfo.length() - "/team".length());
            Long targetUserId = parsePositiveId(sub);
            if (targetUserId != null) {
                handleAssignTeam(request, response, actorUserId, targetUserId);
                return;
            }
        }

        writeJson(response, HttpServletResponse.SC_NOT_FOUND, false, "Endpoint không tồn tại", null);
    }

    /**
     * PUT /api/roles/user/{id}/add — add a single role to a user.
     * Body: { "roleId": Long }
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        if (!hasAdminOrDirectorRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền cập nhật vai trò người dùng", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        Long targetUserId = parseSubPathUserId(pathInfo, "/user/", "/add");
        if (targetUserId == null) {
            targetUserId = parseUserPath(pathInfo, "/user/");
        }

        if (targetUserId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "ID người dùng không hợp lệ", null);
            return;
        }

        SingleRoleRequest body;
        try {
            body = GSON.fromJson(request.getReader(), SingleRoleRequest.class);
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
            return;
        }

        if (body == null || body.roleId() == null || body.roleId() <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "roleId không hợp lệ", null);
            return;
        }

        try {
            RoleAssignmentResult result = userRoleService.addRole(actorUserId, targetUserId, body.roleId());
            handleRoleResult(response, result, targetUserId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-29: Error adding role " + body.roleId() + " to user " + targetUserId, e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi thêm vai trò", null);
        }
    }

    /**
     * DELETE /api/roles/user/{id}/remove — remove a single role from a user.
     * Body: { "roleId": Long }
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long actorUserId = extractActorUserId(request);
        if (actorUserId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        if (!hasAdminOrDirectorRole(request)) {
            writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                    "Không có quyền gỡ vai trò người dùng", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        Long targetUserId = parseSubPathUserId(pathInfo, "/user/", "/remove");
        if (targetUserId == null) {
            targetUserId = parseUserPath(pathInfo, "/user/");
        }

        if (targetUserId == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "ID người dùng không hợp lệ", null);
            return;
        }

        SingleRoleRequest body;
        try {
            body = GSON.fromJson(request.getReader(), SingleRoleRequest.class);
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
            return;
        }

        if (body == null || body.roleId() == null || body.roleId() <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "roleId không hợp lệ", null);
            return;
        }

        try {
            RoleAssignmentResult result = userRoleService.removeRole(actorUserId, targetUserId, body.roleId());
            handleRoleResult(response, result, targetUserId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-29: Error removing role " + body.roleId() + " from user " + targetUserId, e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi gỡ vai trò", null);
        }
    }

    // === Handlers ===

    private void handleAssignRolesAndTeam(HttpServletRequest request, HttpServletResponse response,
                                          long actorUserId) throws IOException {
        RoleAssignRequest body;
        try {
            body = GSON.fromJson(request.getReader(), RoleAssignRequest.class);
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
            return;
        }

        if (body == null || body.userId() == null || body.userId() <= 0 || body.roleIds() == null) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Dữ liệu phân vai trò không hợp lệ: thiếu userId hoặc roleIds", null);
            return;
        }

        try {
            // RULE 1: Multi-role — assign all listed roleIds
            // If teamId is also supplied, it's assigned atomically
            RoleAssignmentResult result = userRoleService.assignRoles(
                    actorUserId, body.userId(), body.roleIds(), body.teamId());

            switch (result) {
                case SUCCESS -> {
                    List<Role> updatedRoles = userRoleService.findRolesByUserId(body.userId());
                    Long updatedTeamId = userRoleService.findTeamIdByUserId(body.userId());
                    writeJson(response, HttpServletResponse.SC_OK, true,
                            "Phân vai trò và nhóm kinh doanh thành công",
                            new UserRolesAndTeamData(body.userId(), updatedRoles, updatedTeamId));
                }

                case INVALID_USER, USER_NOT_FOUND -> writeJson(response,
                        HttpServletResponse.SC_NOT_FOUND, false, "Không tìm thấy người dùng", null);

                case INVALID_ROLE -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Một hoặc nhiều vai trò không hợp lệ hoặc không tồn tại trong hệ thống", null);

                // RULE 2 violation: Team Lead must belong to a team
                case TEAM_REQUIRED -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Vai trò 'Team Lead' (Trưởng nhóm) bắt buộc người dùng phải thuộc về ít nhất một nhóm kinh doanh.",
                        null);

                // RULE 3 violation: Admin cannot revoke their own Admin role
                case CANNOT_REVOKE_OWN_ADMIN -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Không thể tự gỡ bỏ vai trò 'Admin' của chính mình. Yêu cầu Admin khác thực hiện thao tác này.",
                        null);

                case TEAM_NOT_FOUND -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Nhóm kinh doanh không tồn tại trong hệ thống", null);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-29: Error assigning roles/team to user " + body.userId(), e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi phân vai trò người dùng", null);
        }
    }

    private void handleAssignTeam(HttpServletRequest request, HttpServletResponse response,
                                  long actorUserId, Long pathUserId) throws IOException {
        TeamAssignRequest body;
        try {
            body = GSON.fromJson(request.getReader(), TeamAssignRequest.class);
        } catch (JsonSyntaxException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "JSON không hợp lệ", null);
            return;
        }

        Long targetUserId = pathUserId != null ? pathUserId : (body != null ? body.userId() : null);
        if (targetUserId == null || targetUserId <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, "userId không hợp lệ", null);
            return;
        }

        Long teamId = body != null ? body.teamId() : null;

        try {
            RoleAssignmentResult result = userRoleService.assignTeam(actorUserId, targetUserId, teamId);
            switch (result) {
                case SUCCESS -> {
                    Long updatedTeamId = userRoleService.findTeamIdByUserId(targetUserId);
                    writeJson(response, HttpServletResponse.SC_OK, true,
                            "Gán nhóm kinh doanh thành công", new UserTeamData(targetUserId, updatedTeamId));
                }

                case INVALID_USER, USER_NOT_FOUND -> writeJson(response,
                        HttpServletResponse.SC_NOT_FOUND, false, "Không tìm thấy người dùng", null);

                // RULE 2 violation: Team Lead cannot be unassigned from team
                case TEAM_REQUIRED -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Người dùng đang giữ vai trò 'Team Lead' (Trưởng nhóm) bắt buộc phải thuộc về ít nhất một nhóm kinh doanh.",
                        null);

                case TEAM_NOT_FOUND -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Nhóm kinh doanh không tồn tại trong hệ thống", null);

                default -> writeJson(response,
                        HttpServletResponse.SC_BAD_REQUEST, false,
                        "Không thể cập nhật nhóm kinh doanh", null);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-29: Error assigning team to user " + targetUserId, e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi gán nhóm kinh doanh", null);
        }
    }

    private void handleRoleResult(HttpServletResponse response, RoleAssignmentResult result,
                                  long targetUserId) throws IOException {
        switch (result) {
            case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                    "Cập nhật vai trò thành công", null);

            case INVALID_USER, USER_NOT_FOUND -> writeJson(response,
                    HttpServletResponse.SC_NOT_FOUND, false, "Không tìm thấy người dùng", null);

            case INVALID_ROLE -> writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST, false,
                    "Vai trò không hợp lệ hoặc không tồn tại trong hệ thống", null);

            // RULE 2 violation
            case TEAM_REQUIRED -> writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST, false,
                    "Vai trò 'Team Lead' (Trưởng nhóm) bắt buộc người dùng phải thuộc về ít nhất một nhóm kinh doanh.",
                    null);

            // RULE 3 violation
            case CANNOT_REVOKE_OWN_ADMIN -> writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST, false,
                    "Không thể tự gỡ bỏ vai trò 'Admin' của chính mình. Yêu cầu Admin khác thực hiện thao tác này.",
                    null);

            case TEAM_NOT_FOUND -> writeJson(response,
                    HttpServletResponse.SC_BAD_REQUEST, false,
                    "Nhóm kinh doanh không tồn tại trong hệ thống", null);
        }
    }

    // === Utility parsing & auth ===

    private Long parseUserPath(String pathInfo, String prefix) {
        if (pathInfo == null || !pathInfo.startsWith(prefix)) {
            return null;
        }
        String rest = pathInfo.substring(prefix.length());
        if (rest.isBlank() || rest.contains("/")) {
            return null;
        }
        return parsePositiveId(rest);
    }

    private Long parseSubPathUserId(String pathInfo, String prefix, String suffix) {
        if (pathInfo == null || !pathInfo.startsWith(prefix) || !pathInfo.endsWith(suffix)) {
            return null;
        }
        String rest = pathInfo.substring(prefix.length(), pathInfo.length() - suffix.length());
        if (rest.isBlank() || rest.contains("/")) {
            return null;
        }
        return parsePositiveId(rest);
    }

    private Long parsePositiveId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
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
            return parsePositiveId(text);
        }
        return null;
    }

    private boolean hasAdminOrDirectorRole(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return false;
        }
        if (session == null) {
            return false;
        }
        try {
            Object rolesValue = session.getAttribute(SessionKey.ROLES);
            if (rolesValue instanceof Collection<?> roles) {
                return roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .map(r -> r.trim().toLowerCase(Locale.ROOT))
                        .anyMatch(r -> "admin".equals(r) || "director".equals(r));
            }
        } catch (IllegalStateException e) {
            return false;
        }
        return false;
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        return extractActorUserId(request) != null;
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    // === DTO records ===

    private record RoleAssignRequest(Long userId, List<Long> roleIds, Long teamId) { }
    private record TeamAssignRequest(Long userId, Long teamId) { }
    private record SingleRoleRequest(Long roleId) { }
    private record UserTeamData(Long userId, Long teamId) { }
    private record UserRolesAndTeamData(Long userId, List<Role> roles, Long teamId) { }
    private record ApiResponse(boolean success, String message, Object data) { }
}
