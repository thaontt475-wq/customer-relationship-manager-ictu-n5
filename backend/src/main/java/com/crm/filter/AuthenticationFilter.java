package com.crm.filter;

import com.crm.dto.common.ApiResponse;
import com.crm.util.ResponseUtil;

import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;

public class AuthenticationFilter
        implements Filter {

    @Override
    public void doFilter(
            ServletRequest request,
            ServletResponse response,
            FilterChain chain
    ) throws IOException, ServletException {

        HttpServletRequest req =
                (HttpServletRequest) request;

        HttpServletResponse res =
                (HttpServletResponse) response;

        String uri =
                req.getRequestURI();

        if (
                uri.endsWith(
                        "/api/auth/login"
                ) ||
                uri.endsWith(
                        "/api/auth/forgot-password"
                ) ||
                uri.endsWith(
                        "/api/auth/reset-password"
                ) ||
                "OPTIONS".equalsIgnoreCase(
                        req.getMethod()
                )
        ) {

            chain.doFilter(
                    request,
                    response
            );

            return;
        }

        HttpSession session =
                req.getSession(false);

        if (
                session == null ||
                session.getAttribute(
                        "userId"
                ) == null
        ) {

            ResponseUtil.json(
                    res,
                    401,
                    ApiResponse.error(
                            "Chưa đăng nhập",
                            null
                    )
            );

            return;
        }

        try {
            var account = new com.crm.service.auth.SessionService().account(
                    ((Number) session.getAttribute("userId")).longValue());
            if (account == null || !"ACTIVE".equals(account.get("status"))
                    || !java.util.Objects.equals(session.getAttribute("sessionVersion"), account.get("sessionVersion"))) {
                session.invalidate();
                ResponseUtil.json(res, 401, ApiResponse.error("Phiên đăng nhập đã hết hiệu lực", null));
                return;
            }
            chain.doFilter(request, response);
        } catch (java.sql.SQLException e) {
            ResponseUtil.json(res, 500, ApiResponse.error("Không thể kiểm tra phiên đăng nhập", null));
        }
    }
}
