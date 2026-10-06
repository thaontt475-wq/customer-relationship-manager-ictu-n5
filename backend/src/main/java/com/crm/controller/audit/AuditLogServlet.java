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
                userId =
                        Long.valueOf(
                                userValue
                        );
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
                            20
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