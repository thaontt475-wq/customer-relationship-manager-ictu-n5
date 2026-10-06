package com.crm.controller.winloss;

import com.crm.dto.common.ApiResponse;
import com.crm.model.winloss.WinLossReason;
import com.crm.service.permissions.PermissionService;
import com.crm.service.winloss.WinLossService;
import com.crm.util.JsonUtil;
import com.crm.util.ResponseUtil;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/win-loss-reasons")
public class WinLossReasonServlet
        extends HttpServlet {

    private final WinLossService service =
            new WinLossService();

    private final PermissionService permissionService =
            new PermissionService();

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        try {

            long userId =
                    currentUserId(request);

            if (
                    !permissionService.hasPermission(
                            userId,
                            "winloss.read"
                    )
            ) {
                forbidden(response);
                return;
            }

            String type =
                    request.getParameter("type");

            Boolean active =
                    parseBoolean(
                            request.getParameter(
                                    "active"
                            )
                    );

            Map<String, Object> data =
                    new LinkedHashMap<>();

            data.put(
                    "reasons",
                    service.getReasons(
                            type,
                            active
                    )
            );

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy danh sách lý do thành công",
                            data
                    )
            );

        } catch (IllegalArgumentException e) {

            badRequest(
                    response,
                    e.getMessage()
            );

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

            long userId =
                    currentUserId(request);

            if (
                    !permissionService.hasPermission(
                            userId,
                            "winloss.manage"
                    )
            ) {
                forbidden(response);
                return;
            }

            WinLossReason reason =
                    JsonUtil.getGson()
                            .fromJson(
                                    request.getReader(),
                                    WinLossReason.class
                            );

            ResponseUtil.json(
                    response,
                    201,
                    ApiResponse.success(
                            "Tạo lý do thành công",
                            service.createReason(
                                    reason
                            )
                    )
            );

        } catch (IllegalArgumentException e) {

            badRequest(
                    response,
                    e.getMessage()
            );

        } catch (Exception e) {

            serverError(
                    response,
                    e
            );
        }
    }

    private long currentUserId(
            HttpServletRequest request
    ) {

        return (Long) request
                .getSession(false)
                .getAttribute("userId");
    }

    private Boolean parseBoolean(
            String value
    ) {

        if (
                value == null ||
                value.isBlank()
        ) {
            return null;
        }

        if (
                !"true".equalsIgnoreCase(value) &&
                !"false".equalsIgnoreCase(value)
        ) {
            throw new IllegalArgumentException(
                    "active không hợp lệ"
            );
        }

        return Boolean.valueOf(value);
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

    private void serverError(
            HttpServletResponse response,
            Exception e
    ) throws IOException {

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