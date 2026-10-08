package com.crm.controller.teams;

import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.PermissionService;
import com.crm.service.teams.TeamService;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/teams")
public class TeamServlet
        extends HttpServlet {

    private final TeamService service =
            new TeamService();

    private final PermissionService permissionService =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            long userId =
                    (Long) request
                            .getSession(false)
                            .getAttribute("userId");

            if (
                    !permissionService.hasPermission(
                            userId,
                            "team.read"
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

            Boolean active = null;

            String activeValue =
                    request.getParameter(
                            "active"
                    );

            if (
                    activeValue != null &&
                    !activeValue.isBlank()
            ) {
                active =
                        Boolean.valueOf(
                                activeValue
                        );
            }

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "teams",
                    service.getTeams(
                            request.getParameter(
                                    "keyword"
                            ),
                            active
                    )
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy team thành công",
                            data
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

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
}