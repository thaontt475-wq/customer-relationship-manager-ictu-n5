package com.crm.filter;

import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;
import com.crm.service.permissions.MenuService;
import com.crm.util.SessionKey;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Filter controlling menu authorization and populating navigation context (CRM-26).
 * Architecture: Filter -> Servlet -> Service -> DAO -> JDBC -> MySQL 8.0.
 *
 * Responsibilities:
 * 1. Checks user authentication session.
 * 2. Injects filtered menu items (AC 1) into request attribute "menuItems".
 * 3. Injects user profile (Name, Role, Team - AC 2) into request attributes.
 * 4. Guards restricted module URLs against unauthorized direct navigation.
 */
@WebFilter(
        urlPatterns = {
                "/dashboard",
                "/organization", "/organization/*",
                "/users", "/users/*",
                "/permissions", "/permissions/*",
                "/products", "/products/*",
                "/customers", "/customers/*",
                "/leads", "/leads/*",
                "/pipeline", "/pipeline/*",
                "/activities", "/activities/*",
                "/quotes", "/quotes/*",
                "/kpi", "/kpi/*",
                "/winloss", "/winloss/*", "/reports", "/reports/*",
                "/automation", "/automation/*",
                "/audit", "/audit/*",
                "/profile", "/profile/*",
                "/change-password",
                "/navigation", "/menu",
                "/errors/*"
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR}
)
public class MenuNavigationFilter implements Filter {
    private static final Logger LOGGER = Logger.getLogger(MenuNavigationFilter.class.getName());
    private final MenuService menuService;

    public MenuNavigationFilter() {
        this.menuService = new MenuService();
    }

    public MenuNavigationFilter(MenuService menuService) {
        this.menuService = menuService != null ? menuService : new MenuService();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = null;
        Long userId = null;
        List<String> roles = new ArrayList<>();

        try {
            session = httpRequest.getSession(false);
            if (session != null) {
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
            }
        } catch (IllegalStateException e) {
            session = null;
        }

        if (session != null && userId != null && userId > 0) {
            // AC 1: Filtered menu items according to roles
            List<MenuItem> menuItems = roles.isEmpty()
                    ? menuService.getMenuItemsByUserId(userId)
                    : menuService.getMenuItems(roles);

            // AC 2: User profile (Name, Role, Team)
            UserNavigationProfile profile = menuService.getUserNavigationProfile(userId);

            httpRequest.setAttribute("menuItems", menuItems);
            httpRequest.setAttribute("userNavigationProfile", profile);

            if (profile != null) {
                httpRequest.setAttribute("currentUserDisplayName", profile.getDisplayName());
                httpRequest.setAttribute("currentUserRoleLabel", profile.getRole());
                httpRequest.setAttribute("currentUserTeamName", profile.getTeamName());
            }

            // Also keep standard Sprint 1 items for older views if needed
            httpRequest.setAttribute("sprint1MenuItems", menuService.getSprint1MenuItems(roles));

            // Module authorization check: prevent direct URL bypass to restricted areas
            String path = httpRequest.getServletPath();
            if (isProtectedAdminOnlyPath(path) && !menuService.isAuthorized(roles, path)) {
                LOGGER.log(Level.WARNING, "CRM-26: Access forbidden for userId {0} to restricted path {1}",
                        new Object[]{userId, path});
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        } else {
            httpRequest.setAttribute("menuItems", List.of());
        }

        chain.doFilter(request, response);
    }

    private boolean isProtectedAdminOnlyPath(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase();
        return lower.startsWith("/users") || lower.startsWith("/permissions") ||
                lower.startsWith("/audit") || lower.startsWith("/automation");
    }
}
