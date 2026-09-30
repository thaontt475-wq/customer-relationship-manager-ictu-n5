package com.crm.filter;

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
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@WebFilter(
        urlPatterns = {
                "/dashboard", "/users", "/users/*", "/permissions", "/permissions/*",
                "/change-password", "/errors/*"
        },
        dispatcherTypes = {DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ERROR}
)
public class SprintNavigationFilter implements Filter {
    private final MenuService menuService = new MenuService();

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpSession session;
        Object currentUser = null;
        try {
            session = httpRequest.getSession(false);
            if (session != null) {
                currentUser = session.getAttribute(SessionKey.CURRENT_USER);
            }
        } catch (IllegalStateException e) {
            session = null;
        }

        if (session == null || currentUser == null) {
            httpRequest.setAttribute("menuItems", List.of());
        } else {
            httpRequest.setAttribute("menuItems",
                    menuService.getSprint1MenuItems(resolveRoles(session)));
        }

        chain.doFilter(request, response);
    }

    private List<String> resolveRoles(HttpSession session) {
        Object value;
        try {
            value = session.getAttribute(SessionKey.ROLES);
        } catch (IllegalStateException e) {
            return List.of();
        }
        if (!(value instanceof Iterable<?> roles)) {
            return List.of();
        }

        List<String> resolved = new ArrayList<>();
        for (Object role : roles) {
            if (role instanceof String text && !text.isBlank()) {
                resolved.add(text);
            }
        }
        return List.copyOf(resolved);
    }
}
