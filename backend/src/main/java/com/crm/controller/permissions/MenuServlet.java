package com.crm.controller.permissions;

import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.MenuService;
import com.crm.service.permissions.PermissionService;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.*;

@WebServlet("/api/navigation/menu")
public class MenuServlet extends HttpServlet {

    private final PermissionService permissionService =
            new PermissionService();

    private final MenuService menuService =
            new MenuService();


    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setHeader(
                "Cache-Control",
                "no-store"
        );

        HttpSession session =
                request.getSession(false);


        if (
                session == null
                ||
                session.getAttribute("userId") == null
        ) {

            ResponseUtil.json(
                    response,
                    401,
                    ApiResponse.error(
                            "Chưa đăng nhập",
                            null
                    )
            );

            return;
        }


        try {

            long userId =
                    ((Number) session
                            .getAttribute("userId"))
                            .longValue();


            Set<String> permissions =
                    permissionService
                            .getPermissions(
                                    userId
                            );


            Map<String, Object> data =
                    new LinkedHashMap<>();


            data.put(
                    "menuItems",
                    menuService.getMenu(
                            permissions
                    )
            );


            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy menu thành công",
                            data
                    )
            );

        } catch (Exception e) {

            e.printStackTrace();

            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error(
                            "Không thể lấy menu điều hướng",
                            null
                    )
            );
        }
    }
}