package com.crm.controller.auth;

import com.crm.dto.common.ApiResponse;
import com.crm.service.auth.SessionService;
import com.crm.util.ResponseUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/api/auth/session")
public class SessionServlet extends HttpServlet {

    public static Map<String, Object> describeWithTimeout(Map<String, Object> account) {
        if (account == null) return null;
        Map<String, Object> enriched = new LinkedHashMap<>(account);
        enriched.put("sessionTimeoutSeconds", 15 * 60);
        return enriched;
    }

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

            Map<String, Object> account =
                    new SessionService()
                            .describe(id);

            ResponseUtil.json(
                    response,
                    200,
                    ApiResponse.success(
                            "Lấy thông tin phiên đăng nhập thành công",
                            describeWithTimeout(account)
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