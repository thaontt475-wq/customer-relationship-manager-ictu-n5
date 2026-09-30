package com.crm.controller.permissions;

import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;
import com.crm.service.permissions.MenuService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Navigation controller servlet handling navigation routing and data forwarding (CRM-26).
 * Follows Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0 architecture.
 */
@WebServlet({"/navigation", "/menu"})
public class NavigationServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final MenuService menuService;

    public NavigationServlet() {
        this.menuService = new MenuService();
    }

    public NavigationServlet(MenuService menuService) {
        this.menuService = menuService != null ? menuService : new MenuService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        Long userId = null;
        List<String> roles = new ArrayList<>();

        if (session != null) {
            try {
                Object rawUserId = session.getAttribute("userId");
                if (rawUserId instanceof Number number) {
                    userId = number.longValue();
                }

                Object sessionRoles = session.getAttribute(SessionKey.ROLES);
                if (sessionRoles == null) {
                    sessionRoles = session.getAttribute("roles");
                }
                if (sessionRoles instanceof Collection<?> roleValues) {
                    for (Object value : roleValues) {
                        if (value instanceof String role && !role.isBlank()) {
                            roles.add(role.trim());
                        }
                    }
                }
            } catch (IllegalStateException ignored) {
                session = null;
            }
        }

        // Check authentication
        if (session == null || userId == null || userId <= 0) {
            String acceptHeader = request.getHeader("Accept");
            if (acceptHeader != null && acceptHeader.contains("application/json")) {
                response.setContentType("application/json");
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                GSON.toJson(new NavigationResponse(false, "Chưa đăng nhập", null), response.getWriter());
            } else {
                response.sendRedirect(request.getContextPath() + "/login?expired=1");
            }
            return;
        }

        // AC 1: Get filtered menu items
        List<MenuItem> menuItems = roles.isEmpty()
                ? menuService.getMenuItemsByUserId(userId)
                : menuService.getMenuItems(roles);

        // AC 2: Get user profile info (Name, Role, Team)
        UserNavigationProfile profile = menuService.getUserNavigationProfile(userId);

        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("application/json")) {
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setStatus(HttpServletResponse.SC_OK);
            GSON.toJson(new NavigationResponse(true, "Lấy điều hướng thành công",
                    new NavigationData(menuItems, profile)), response.getWriter());
            return;
        }

        // Forward to dashboard with populated navigation attributes
        request.setAttribute("menuItems", menuItems);
        request.setAttribute("userNavigationProfile", profile);
        if (profile != null) {
            request.setAttribute("currentUserDisplayName", profile.getDisplayName());
            request.setAttribute("currentUserRoleLabel", profile.getRole());
            request.setAttribute("currentUserTeamName", profile.getTeamName());
        }

        // Direct user to dashboard or first permitted menu item
        String targetUrl = "/dashboard";
        if (!menuItems.isEmpty() && menuItems.get(0).getUrl() != null) {
            String firstUrl = menuItems.get(0).getUrl();
            if (!firstUrl.isBlank()) {
                targetUrl = firstUrl;
            }
        }

        request.getRequestDispatcher(targetUrl).forward(request, response);
    }

    public record NavigationResponse(boolean success, String message, NavigationData data) { }
    public record NavigationData(List<MenuItem> menuItems, UserNavigationProfile profile) { }
}
