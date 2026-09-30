package com.crm.controller.permissions;

import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;
import com.crm.service.permissions.MenuService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
 * Controller endpoint providing role-based navigation menu and user profile (CRM-26).
 * Follows Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0 architecture.
 */
@WebServlet({"/api/navigation/menu", "/api/menu"})
public class MenuServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private final MenuService menuService;

    public MenuServlet() {
        this.menuService = new MenuService();
    }

    public MenuServlet(MenuService menuService) {
        this.menuService = menuService != null ? menuService : new MenuService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

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
                // Session may be invalidated concurrently
                writeUnauthorized(response);
                return;
            }
        }

        if (userId == null || userId <= 0) {
            writeUnauthorized(response);
            return;
        }

        // AC 1: Filter menu items by roles. If session roles empty, fallback to querying DB roles via DAO
        List<MenuItem> menuItems;
        if (!roles.isEmpty()) {
            menuItems = menuService.getMenuItems(roles);
        } else {
            menuItems = menuService.getMenuItemsByUserId(userId);
        }

        // AC 2: User profile with name, role, team
        UserNavigationProfile userProfile = menuService.getUserNavigationProfile(userId);

        response.setStatus(HttpServletResponse.SC_OK);
        GSON.toJson(new MenuResponse(true, "Lấy menu thành công",
                new MenuData(menuItems, userProfile)), response.getWriter());
    }

    private static void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        GSON.toJson(new MenuResponse(false, "Chưa đăng nhập", null), response.getWriter());
    }

    public record MenuResponse(boolean success, String message, MenuData data) { }
    public record MenuData(List<MenuItem> menuItems, UserNavigationProfile userProfile) { }
}
