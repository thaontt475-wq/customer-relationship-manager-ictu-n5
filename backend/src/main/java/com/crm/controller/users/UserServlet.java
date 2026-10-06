package com.crm.controller.users;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.users.*;
import com.crm.service.permissions.PermissionService;
import com.crm.service.teams.TeamService;
import com.crm.service.users.UserManagementService;
import com.crm.service.users.UserStatusService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.NoSuchElementException;

@WebServlet({
        "/api/users",
        "/api/users/*"
})
public class UserServlet
        extends HttpServlet {

    private final UserManagementService users =
            new UserManagementService();

    private final UserStatusService statusService =
            new UserStatusService();

    private final TeamService teamService =
            new TeamService();

    private final PermissionService permissions =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            require(
                    request,
                    "user.read"
            );

            String path =
                    request.getPathInfo();

            if (
                    path == null ||
                    path.equals("/")
            ) {

                int page =
                        intParam(
                                request,
                                "page",
                                1
                        );

                int size =
                        intParam(
                                request,
                                "size",
                                20
                        );

                ResponseUtil.json(
                        response,
                        200,
                        ApiResponse.success(
                                "Lấy danh sách user thành công",
                                users.search(
                                        request.getParameter(
                                                "keyword"
                                        ),
                                        request.getParameter(
                                                "status"
                                        ),
                                        page,
                                        size
                                )
                        )
                );

                return;
            }

            long id =
                    pathId(path);

            var user =
                    users.get(id);

            if (user == null) {
                notFound(response);
                return;
            }

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy user thành công",
                            user
                    )
            );

        } catch (SecurityException e) {
            forbidden(response);
        } catch (IllegalArgumentException e) {
            badRequest(response, e.getMessage());
        } catch (com.google.gson.JsonParseException e) {
            badRequest(response, "JSON không hợp lệ");
        } catch (Exception e) {
            serverError(response, e);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            String path =
                    request.getPathInfo();

            if (
                    path == null ||
                    path.equals("/")
            ) {

                require(
                        request,
                        "user.create"
                );

                UserWriteRequest body =
                        JsonUtil.getGson()
                                .fromJson(
                                        request.getReader(),
                                        UserWriteRequest.class
                                );

                if (body != null && "LOCKED".equalsIgnoreCase(body.getStatus())) require(request, "user.lock");
                ResponseUtil.json(
                        response,
                        201,
                        ApiResponse.success(
                                "Tạo user thành công",
                                users.create(body)
                        )
                );

                return;
            }

            String[] segments =
                    segments(path);

            if (segments.length != 2) {
                notFound(response);
                return;
            }

            long targetId =
                    Long.parseLong(
                            segments[0]
                    );

            long currentUserId =
                    currentUser(request);

            switch (segments[1]) {

                case "team" -> {

                    require(
                            request,
                            "team.assign"
                    );

                    TeamAssignRequest body =
                            JsonUtil.getGson()
                                    .fromJson(
                                            request.getReader(),
                                            TeamAssignRequest.class
                                    );

                    ResponseUtil.json(
                            response,
                            200,
                            ApiResponse.success(
                                    "Gán team thành công",
                                    teamService.assignTeam(
                                            targetId,
                                            body == null
                                                    ? null
                                                    : body.getTeamId()
                                    )
                            )
                    );
                }

                case "lock" -> {

                    require(
                            request,
                            "user.lock"
                    );

                    LockRequest body =
                            JsonUtil.getGson()
                                    .fromJson(
                                            request.getReader(),
                                            LockRequest.class
                                    );

                    ResponseUtil.json(
                            response,
                            200,
                            ApiResponse.success(
                                    "Khóa tài khoản thành công",
                                    statusService.lock(
                                            targetId,
                                            currentUserId,
                                            body == null
                                                    ? null
                                                    : body.getReason()
                                    )
                            )
                    );
                }

                case "unlock" -> {

                    require(
                            request,
                            "user.lock"
                    );

                    ResponseUtil.json(
                            response,
                            200,
                            ApiResponse.success(
                                    "Mở khóa tài khoản thành công",
                                    statusService.unlock(
                                            targetId,
                                            currentUserId
                                    )
                            )
                    );
                }

                case "transfer-data" -> {

                    require(
                            request,
                            "user.transfer"
                    );

                    TransferDataRequest body =
                            JsonUtil.getGson()
                                    .fromJson(
                                            request.getReader(),
                                            TransferDataRequest.class
                                    );

                    if (body == null) {
                        throw new IllegalArgumentException(
                                "Thiếu toUserId"
                        );
                    }

                    ResponseUtil.json(
                            response,
                            200,
                            ApiResponse.success(
                                    "Bàn giao dữ liệu thành công",
                                    statusService.transfer(
                                            targetId,
                                            body.getToUserId(),
                                            currentUserId
                                    )
                            )
                    );
                }

                default ->
                        notFound(response);
            }

        } catch (SecurityException e) {
            forbidden(response);
        } catch (IllegalStateException e) {
            conflict(response, e.getMessage());
        } catch (
                IllegalArgumentException |
                NoSuchElementException e
        ) {
            badRequest(response, e.getMessage());
        } catch (com.google.gson.JsonParseException e) {
            badRequest(response, "JSON không hợp lệ");
        } catch (Exception e) {
            serverError(response, e);
        }
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            require(
                    request,
                    "user.update"
            );

            long id =
                    pathId(
                            request.getPathInfo()
                    );

            UserWriteRequest body =
                    JsonUtil.getGson()
                            .fromJson(
                                    request.getReader(),
                                    UserWriteRequest.class
                            );

            var existing = users.get(id);
            if (body != null && existing != null && body.getStatus() != null
                    && !body.getStatus().equalsIgnoreCase((String) existing.get("status"))
                    && ("LOCKED".equalsIgnoreCase(body.getStatus()) || "LOCKED".equals(existing.get("status")))) {
                require(request, "user.lock");
            }
            var result =
                    users.update(
                            id,
                            body,
                            (Long) request.getSession(false).getAttribute("userId")
                    );

            if (result == null) {
                notFound(response);
                return;
            }

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Cập nhật user thành công",
                            result
                    )
            );

        } catch (SecurityException e) {
            forbidden(response);
        } catch (IllegalStateException e) {
            conflict(response, e.getMessage());
        } catch (IllegalArgumentException e) {
            badRequest(response, e.getMessage());
        } catch (com.google.gson.JsonParseException e) {
            badRequest(response, "JSON không hợp lệ");
        } catch (Exception e) {
            serverError(response, e);
        }
    }

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            require(
                    request,
                    "user.delete"
            );

            long id =
                    pathId(
                            request.getPathInfo()
                    );

            users.delete(
                    id,
                    currentUser(request)
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Xóa user thành công",
                            null
                    )
            );

        } catch (SecurityException e) {
            forbidden(response);
        } catch (
                IllegalArgumentException |
                NoSuchElementException e
        ) {
            badRequest(response, e.getMessage());
        } catch (com.google.gson.JsonParseException e) {
            badRequest(response, "JSON không hợp lệ");
        } catch (Exception e) {
            serverError(response, e);
        }
    }

    private void require(
            HttpServletRequest request,
            String permission
    ) throws Exception {

        if (
                !permissions.hasPermission(
                        currentUser(request),
                        permission
                )
        ) {
            throw new SecurityException();
        }
    }

    private long currentUser(
            HttpServletRequest request
    ) {

        return (Long) request
                .getSession(false)
                .getAttribute("userId");
    }

    private long pathId(
            String path
    ) {

        if (
                path == null ||
                path.equals("/")
        ) {
            throw new IllegalArgumentException(
                    "Thiếu user id"
            );
        }

        String[] parts =
                segments(path);

        if (parts.length != 1) {
            throw new IllegalArgumentException(
                    "Đường dẫn không hợp lệ"
            );
        }

        return Long.parseLong(
                parts[0]
        );
    }

    private String[] segments(
            String path
    ) {

        return path
                .replaceFirst("^/", "")
                .split("/");
    }

    private int intParam(
            HttpServletRequest request,
            String name,
            int defaultValue
    ) {

        String value =
                request.getParameter(name);

        if (
                value == null ||
                value.isBlank()
        ) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    private void forbidden(
            HttpServletResponse response
    ) throws IOException {

        ResponseUtil.json(
                response,
                403,
                ApiResponse.error(
                        "Không có quyền",
                        null
                )
        );
    }

    private void badRequest(
            HttpServletResponse response,
            String message
    ) throws IOException {

        ResponseUtil.json(
                response,
                400,
                ApiResponse.error(
                        message,
                        null
                )
        );
    }

    private void conflict(
            HttpServletResponse response,
            String message
    ) throws IOException {

        ResponseUtil.json(
                response,
                409,
                ApiResponse.error(
                        message,
                        null
                )
        );
    }

    private void notFound(
            HttpServletResponse response
    ) throws IOException {

        ResponseUtil.json(
                response,
                404,
                ApiResponse.error(
                        "Không tìm thấy dữ liệu",
                        null
                )
        );
    }

    private void serverError(
            HttpServletResponse response,
            Exception e
    ) throws IOException {

        System.err.println("API request failed: " + e.getClass().getSimpleName());

        ResponseUtil.json(
                response,
                500,
                ApiResponse.error(
                        "Lỗi hệ thống",
                        null
                )
        );
    }
}

