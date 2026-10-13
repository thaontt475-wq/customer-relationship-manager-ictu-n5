package com.crm.controller.teams;

import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.PermissionService;
import com.crm.service.teams.TeamService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import com.google.gson.reflect.TypeToken;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet({"/api/teams", "/api/teams/*"})
public class TeamServlet extends HttpServlet {

    private final TeamService service = new TeamService();
    private final PermissionService permissionService = new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            long userId = getSessionUserId(request);

            if (!hasAnyPermission(userId, "team.read", "organization.read", "user.read")) {
                ResponseUtil.json(
                        response,
                        403,
                        ApiResponse.error("Không có quyền", null)
                );
                return;
            }

            String pathInfo = request.getPathInfo();

            // Case: /api/teams/lookups
            if (pathInfo != null && (pathInfo.equals("/lookups") || pathInfo.equals("/lookups/"))) {
                ResponseUtil.json(
                        response,
                        200,
                        ApiResponse.success("Lấy danh mục thành công", service.getLookups())
                );
                return;
            }

            // Case: /api/teams/{id}/members
            if (pathInfo != null && pathInfo.matches("^/\\d+/members/?$")) {
                long teamId = Long.parseLong(pathInfo.split("/")[1]);
                ResponseUtil.json(
                        response,
                        200,
                        ApiResponse.success("Lấy thành viên nhóm thành công", service.getTeamMembers(teamId))
                );
                return;
            }

            // Case: /api/teams/{id}
            if (pathInfo != null && pathInfo.matches("^/\\d+/?$")) {
                long teamId = Long.parseLong(pathInfo.substring(1).replace("/", ""));
                ResponseUtil.json(
                        response,
                        200,
                        ApiResponse.success("Lấy thông tin nhóm thành công", service.getTeamById(teamId))
                );
                return;
            }

            // Case: /api/teams (list)
            Boolean active = null;
            String activeValue = request.getParameter("active");
            if (activeValue != null && !activeValue.isBlank()) {
                active = Boolean.valueOf(activeValue);
            }

            Long regionId = null;
            String regionVal = request.getParameter("regionId");
            if (regionVal != null && !regionVal.isBlank()) {
                try {
                    regionId = Long.valueOf(regionVal);
                } catch (NumberFormatException ignored) {
                }
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put(
                    "teams",
                    service.getTeams(
                            request.getParameter("keyword"),
                            active,
                            regionId
                    )
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success("Lấy danh sách nhóm thành công", data)
            );

        } catch (SecurityException e) {
            ResponseUtil.json(
                    response,
                    401,
                    ApiResponse.error("Chưa đăng nhập", null)
            );
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(e.getMessage(), null)
            );
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error("Lỗi hệ thống", null)
            );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            long userId = getSessionUserId(request);

            if (!hasAnyPermission(userId, "team.manage", "team.assign", "organization.manage")) {
                ResponseUtil.json(
                        response,
                        403,
                        ApiResponse.error("Không có quyền tạo nhóm kinh doanh", null)
                );
                return;
            }

            Map<String, Object> body = JsonUtil.getGson().fromJson(
                    request.getReader(),
                    new TypeToken<Map<String, Object>>() {}.getType()
            );

            if (body == null) {
                ResponseUtil.json(
                        response,
                        400,
                        ApiResponse.error("Dữ liệu không hợp lệ", null)
                );
                return;
            }

            Map<String, Object> created = service.createTeam(body);

            ResponseUtil.json(
                    response,
                    201,
                    ApiResponse.success("Tạo nhóm kinh doanh thành công", created)
            );

        } catch (SecurityException e) {
            ResponseUtil.json(
                    response,
                    401,
                    ApiResponse.error("Chưa đăng nhập", null)
            );
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(e.getMessage(), null)
            );
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error("Lỗi hệ thống", null)
            );
        }
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            long userId = getSessionUserId(request);

            if (!hasAnyPermission(userId, "team.manage", "team.assign", "organization.manage")) {
                ResponseUtil.json(
                        response,
                        403,
                        ApiResponse.error("Không có quyền cập nhật nhóm kinh doanh", null)
                );
                return;
            }

            long teamId = extractIdFromPath(request);

            Map<String, Object> body = JsonUtil.getGson().fromJson(
                    request.getReader(),
                    new TypeToken<Map<String, Object>>() {}.getType()
            );

            if (body == null) {
                ResponseUtil.json(
                        response,
                        400,
                        ApiResponse.error("Dữ liệu không hợp lệ", null)
                );
                return;
            }

            Map<String, Object> updated = service.updateTeam(teamId, body);

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success("Cập nhật nhóm kinh doanh thành công", updated)
            );

        } catch (SecurityException e) {
            ResponseUtil.json(
                    response,
                    401,
                    ApiResponse.error("Chưa đăng nhập", null)
            );
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(e.getMessage(), null)
            );
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error("Lỗi hệ thống", null)
            );
        }
    }

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {
            long userId = getSessionUserId(request);

            if (!hasAnyPermission(userId, "team.manage", "organization.manage")) {
                ResponseUtil.json(
                        response,
                        403,
                        ApiResponse.error("Không có quyền xóa nhóm kinh doanh", null)
                );
                return;
            }

            long teamId = extractIdFromPath(request);
            service.deleteTeam(teamId);

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success("Xóa nhóm kinh doanh thành công", null)
            );

        } catch (SecurityException e) {
            ResponseUtil.json(
                    response,
                    401,
                    ApiResponse.error("Chưa đăng nhập", null)
            );
        } catch (IllegalArgumentException e) {
            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(e.getMessage(), null)
            );
        } catch (Exception e) {
            e.printStackTrace();
            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error("Lỗi hệ thống", null)
            );
        }
    }

    private long getSessionUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw new SecurityException("Unauthenticated");
        }
        return ((Number) session.getAttribute("userId")).longValue();
    }

    private boolean hasAnyPermission(long userId, String... perms) throws Exception {
        for (String perm : perms) {
            if (permissionService.hasPermission(userId, perm)) {
                return true;
            }
        }
        return false;
    }

    private long extractIdFromPath(HttpServletRequest request) {
        String path = request.getPathInfo();
        if (path == null || path.isBlank() || path.equals("/")) {
            throw new IllegalArgumentException("ID nhóm kinh doanh không hợp lệ");
        }
        try {
            return Long.parseLong(path.replace("/", "").trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID nhóm kinh doanh không hợp lệ");
        }
    }
}