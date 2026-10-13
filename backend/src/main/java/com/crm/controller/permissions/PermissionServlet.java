package com.crm.controller.permissions;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.permissions.AssignPermissionRequest;
import com.crm.service.permissions.PermissionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;

@WebServlet({
        "/api/permissions/users/*",
        "/api/permissions/assign"
})
public class PermissionServlet
        extends HttpServlet {

    private final PermissionService service =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            long currentUserId =
                    currentUser(request);

            if (
                    !service.hasPermission(
                            currentUserId,
                            "permission.read"
                    )
            ) {
                forbidden(response);
                return;
            }

            long userId =
                    pathId(request);

            Map<String, Object> data =
                    service.getUserPermissions(
                            userId
                    );

            if (data == null) {

                ResponseUtil.json(
                        response,
                        404,
                        ApiResponse.error(
                                "User khÃ´ng tá»“n táº¡i",
                                null
                        )
                );

                return;
            }

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Láº¥y phÃ¢n quyá»n thÃ nh cÃ´ng",
                            data
                    )
            );

        } catch (IllegalArgumentException e) {

            badRequest(
                    response,
                    e.getMessage()
            );

        } catch (com.google.gson.JsonParseException e) {
            badRequest(response, "JSON khÃ´ng há»£p lá»‡");
        } catch (Exception e) {

            serverError(
                    response,
                    e
            );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            if (
                    !request.getRequestURI()
                            .endsWith(
                                    "/api/permissions/assign"
                            )
            ) {

                response.sendError(404);
                return;
            }

            long currentUserId =
                    currentUser(request);

            if (
                    !service.hasPermission(
                            currentUserId,
                            "permission.manage"
                    )
            ) {
                forbidden(response);
                return;
            }

            AssignPermissionRequest body =
                    JsonUtil.getGson()
                            .fromJson(
                                    request.getReader(),
                                    AssignPermissionRequest.class
                            );

            if (body == null) {
                throw new IllegalArgumentException(
                        "Dá»¯ liá»‡u khÃ´ng há»£p lá»‡"
                );
            }

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "GÃ¡n quyá»n thÃ nh cÃ´ng",
                            service.assign(
                                    body.getUserId(),
                                    body.getRoleIds(),
                                    body.getDataScope(),
                                    currentUserId
                            )
                    )
            );

        } catch (IllegalArgumentException e) {

            badRequest(
                    response,
                    e.getMessage()
            );

        } catch (com.google.gson.JsonParseException e) {
            badRequest(response, "JSON khÃ´ng há»£p lá»‡");
        } catch (Exception e) {

            serverError(
                    response,
                    e
            );
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
            HttpServletRequest request
    ) {

        String path =
                request.getPathInfo();

        if (
                path == null ||
                path.length() <= 1
        ) {
            throw new IllegalArgumentException(
                    "Thiáº¿u userId"
            );
        }

        try {
            return Long.parseLong(
                    path.substring(1)
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "userId khÃ´ng há»£p lá»‡"
            );
        }
    }

    private void forbidden(
            HttpServletResponse response
    ) throws IOException {

        ResponseUtil.json(
                response,
                403,
                ApiResponse.error(
                        "KhÃ´ng cÃ³ quyá»n",
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

    private void serverError(
            HttpServletResponse response,
            Exception e
    ) throws IOException {

        e.printStackTrace(System.err);

        ResponseUtil.json(
                response,
                500,
                ApiResponse.error(
                        "Lá»—i há»‡ thá»‘ng",
                        null
                )
        );
    }
}
