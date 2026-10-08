package com.crm.controller.organization;

import com.crm.dto.common.ApiResponse;
import com.crm.dto.organization.OrganizationUnitRequest;
import com.crm.service.organization.OrganizationService;
import com.crm.service.permissions.PermissionService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet({
        "/api/organization/units",
        "/api/organization/units/*"
})
public class OrganizationServlet
        extends HttpServlet {

    private final OrganizationService service =
            new OrganizationService();

    private final PermissionService permissions =
            new PermissionService();


    @Override
    protected void doGet(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(
                    req,
                    "organization.read"
            );

            Long parentId =
                    parseLongNullable(
                            req.getParameter(
                                    "parentId"
                            )
                    );

            Boolean active =
                    parseBooleanNullable(
                            req.getParameter(
                                    "active"
                            )
                    );

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Lấy cơ cấu tổ chức thành công",
                            service.getUnits(
                                    parentId,
                                    active
                            )
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            serverError(
                    res,
                    e
            );
        }
    }


    @Override
    protected void doPost(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(
                    req,
                    "organization.manage"
            );

            OrganizationUnitRequest body =
                    JsonUtil.getGson()
                            .fromJson(
                                    req.getReader(),
                                    OrganizationUnitRequest.class
                            );

            ResponseUtil.json(
                    res,
                    201,
                    ApiResponse.success(
                            "Tạo đơn vị thành công",
                            service.create(body)
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            serverError(
                    res,
                    e
            );
        }
    }


    @Override
    protected void doPut(
            HttpServletRequest req,
            HttpServletResponse res
    ) throws IOException {

        try {

            require(
                    req,
                    "organization.manage"
            );

            long id =
                    pathId(req);

            OrganizationUnitRequest body =
                    JsonUtil.getGson()
                            .fromJson(
                                    req.getReader(),
                                    OrganizationUnitRequest.class
                            );

            ResponseUtil.json(
                    res,
                    200,
                    ApiResponse.success(
                            "Cập nhật đơn vị thành công",
                            service.update(
                                    id,
                                    body
                            )
                    )
            );

        } catch (SecurityException e) {

            forbidden(res);

        } catch (IllegalArgumentException e) {

            badRequest(
                    res,
                    e.getMessage()
            );

        } catch (Exception e) {

            serverError(
                    res,
                    e
            );
        }
    }


    private long pathId(
            HttpServletRequest req
    ) {

        String path =
                req.getPathInfo();

        if (
                path == null
                ||
                path.equals("/")
        ) {
            throw new IllegalArgumentException(
                    "ID không hợp lệ"
            );
        }

        try {

            return Long.parseLong(
                    path.substring(1)
            );

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "ID không hợp lệ"
            );
        }
    }


    private Long parseLongNullable(
            String value
    ) {

        if (
                value == null
                ||
                value.isBlank()
        ) {
            return null;
        }

        try {

            return Long.valueOf(value);

        } catch (NumberFormatException e) {

            throw new IllegalArgumentException(
                    "parentId không hợp lệ"
            );
        }
    }


    private Boolean parseBooleanNullable(
            String value
    ) {

        if (
                value == null
                ||
                value.isBlank()
        ) {
            return null;
        }

        if (
                value.equalsIgnoreCase("true")
        ) {
            return true;
        }

        if (
                value.equalsIgnoreCase("false")
        ) {
            return false;
        }

        throw new IllegalArgumentException(
                "active không hợp lệ"
        );
    }


    private void require(
            HttpServletRequest req,
            String permission
    ) throws Exception {

        HttpSession session =
                req.getSession(false);

        if (
                session == null
                ||
                session.getAttribute(
                        "userId"
                ) == null
        ) {
            throw new SecurityException();
        }

        long userId =
                ((Number) session.getAttribute(
                        "userId"
                ))
                        .longValue();

        if (
                !permissions.hasPermission(
                        userId,
                        permission
                )
        ) {
            throw new SecurityException();
        }
    }


    private void forbidden(
            HttpServletResponse res
    ) throws IOException {

        ResponseUtil.json(
                res,
                403,
                ApiResponse.error(
                        "Không có quyền",
                        null
                )
        );
    }


    private void badRequest(
            HttpServletResponse res,
            String message
    ) throws IOException {

        ResponseUtil.json(
                res,
                400,
                ApiResponse.error(
                        message,
                        null
                )
        );
    }


    private void serverError(
            HttpServletResponse res,
            Exception e
    ) throws IOException {

        e.printStackTrace();

        ResponseUtil.json(
                res,
                500,
                ApiResponse.error(
                        "Lỗi hệ thống",
                        null
                )
        );
    }
}