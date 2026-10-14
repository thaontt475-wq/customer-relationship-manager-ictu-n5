package com.crm.filter;

import com.crm.dto.common.ApiResponse;
import com.crm.service.permissions.MenuService;
import com.crm.util.ResponseUtil;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter phân quyền và tiền xử lý cho luồng điều hướng menu (CRM-26).
 * Quy trình: Filter (Kiểm tra phiên đăng nhập, phân quyền, mã hóa UTF-8, log)
 *            -> Servlet (MenuServlet)
 *            -> Service (MenuService)
 *            -> DAO (MenuDAO)
 *            -> JDBC -> MySQL 8.0.
 */
@WebFilter(urlPatterns = {"/api/navigation/*", "/api/menu/*", "/api/menu"})
public class NavigationFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(NavigationFilter.class.getName());

    private MenuService menuService;

    @Override
    public void init(FilterConfig filterConfig) {
        this.menuService = new MenuService();
        LOGGER.info("NavigationFilter initialized successfully.");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Xử lý mã hóa chuẩn UTF-8
        httpRequest.setCharacterEncoding(StandardCharsets.UTF_8.name());
        httpResponse.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String uri = httpRequest.getRequestURI();
        String method = httpRequest.getMethod();

        LOGGER.log(Level.FINE, "Navigation request received: [{0}] {1}", new Object[]{method, uri});

        // Bỏ qua pre-flight CORS requests
        if ("OPTIONS".equalsIgnoreCase(method)) {
            chain.doFilter(request, response);
            return;
        }

        // Kiểm tra phiên đăng nhập
        HttpSession session = httpRequest.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            LOGGER.log(Level.WARNING, "Unauthorized access attempt to navigation route: {0}", uri);
            ResponseUtil.json(
                    httpResponse,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    ApiResponse.error("Chưa đăng nhập", null)
            );
            return;
        }

        try {
            long userId = ((Number) session.getAttribute("userId")).longValue();

            // Tiền xử lý gán thông tin phân quyền & menu vào request attribute
            if (menuService != null) {
                var navigationData = menuService.getNavigationData(userId);
                httpRequest.setAttribute("menuItems", navigationData.get("menuItems"));
                httpRequest.setAttribute("userNavigationProfile", navigationData.get("user"));
                httpRequest.setAttribute("navigationPermissions", navigationData.get("permissions"));
            }

            chain.doFilter(request, response);

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Lỗi xảy ra trong quá trình xử lý NavigationFilter", e);
            ResponseUtil.json(
                    httpResponse,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    ApiResponse.error("Lỗi xử lý phân quyền điều hướng", null)
            );
        }
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
