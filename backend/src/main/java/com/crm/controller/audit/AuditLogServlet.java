package com.crm.controller.audit;

import com.crm.dto.common.ApiResponse;
import com.crm.service.audit.AuditLogService;
import com.crm.service.permissions.PermissionService;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/audit-logs")
public class AuditLogServlet
        extends HttpServlet {

    private final AuditLogService service =
            new AuditLogService();

    private final PermissionService permissions =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            long currentUserId =
                    (Long) request
                            .getSession(false)
                            .getAttribute("userId");

            if (
                    !permissions.hasPermission(
                            currentUserId,
                            "audit.read"
                    )
            ) {

                ResponseUtil.json(
                        response,
                        403,
                        ApiResponse.error(
                                "Không có quyền",
                                null
                        )
                );

                return;
            }

            Long userId = null;

            String userValue =
                    request.getParameter("user");

            if (
                    userValue != null &&
                    !userValue.isBlank()
            ) {
                try {
                    userId = Long.valueOf(userValue);
                } catch (NumberFormatException ignored) {
                    // Nếu truyền tên thay vì ID thì bỏ qua filter ID để tìm kiếm trên client
                }
            }

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
                            50
                    );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy audit log thành công",
                            service.search(
                                    request.getParameter(
                                            "entity"
                                    ),
                                    request.getParameter(
                                            "action"
                                    ),
                                    userId,
                                    request.getParameter(
                                            "from"
                                    ),
                                    request.getParameter(
                                            "to"
                                    ),
                                    page,
                                    size
                            )
                    )
            );

        } catch (Exception e) {

            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(
                            e.getMessage(),
                            null
                    )
            );
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            Long currentUserId = null;
            HttpSession session = request.getSession(false);
            if (session != null && session.getAttribute("userId") != null) {
                currentUserId = (Long) session.getAttribute("userId");
            }

            java.util.Map<String, Object> body =
                    com.crm.util.JsonUtil.getGson()
                            .fromJson(
                                    request.getReader(),
                                    java.util.Map.class
                            );

            if (body == null) {
                throw new IllegalArgumentException("Dữ liệu trống");
            }

            String entity = (String) (body.get("entity") != null ? body.get("entity") : body.get("objectType"));
            String entityId = body.get("entityId") != null ? String.valueOf(body.get("entityId")) : (body.get("objectId") != null ? String.valueOf(body.get("objectId")) : "0");
            String action = (String) body.get("action");
            String description = (String) body.get("description");
            String beforeValue = body.get("beforeValue") != null ? String.valueOf(body.get("beforeValue")) : (body.get("before_value") != null ? String.valueOf(body.get("before_value")) : null);
            String afterValue = body.get("afterValue") != null ? String.valueOf(body.get("afterValue")) : (body.get("after_value") != null ? String.valueOf(body.get("after_value")) : null);

            if (entity == null || entity.isBlank()) {
                entity = "GENERAL";
            }
            if (action == null || action.isBlank()) {
                action = "UPDATE";
            }

            service.log(
                    currentUserId != null ? currentUserId : 1L,
                    entity,
                    entityId,
                    action,
                    description,
                    beforeValue,
                    afterValue
            );

            ResponseUtil.json(
                    response,
                    201,
                    ApiResponse.success(
                            "Ghi audit log thành công",
                            null
                    )
            );

        } catch (Exception e) {

            ResponseUtil.json(
                    response,
                    400,
                    ApiResponse.error(
                            e.getMessage(),
                            null
                    )
            );
        }
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
}