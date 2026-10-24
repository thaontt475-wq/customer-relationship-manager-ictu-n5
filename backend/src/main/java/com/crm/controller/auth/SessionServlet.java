package com.crm.controller.auth;

import com.crm.dto.common.ApiResponse;
import com.crm.service.auth.SessionService;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;

@WebServlet("/api/auth/session")
public class SessionServlet extends HttpServlet {

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
                session == null ||
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

            long id =
                    ((Number) session
                            .getAttribute("userId"))
                            .longValue();

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy thông tin phiên đăng nhập thành công",
                            new SessionService()
                                    .describe(id)
                    )
            );

        } catch (Exception e) {

            ResponseUtil.json(
                    response,
                    500,
                    ApiResponse.error(
                            "Không thể lấy thông tin phiên",
                            null
                    )
            );
        }
    }
}