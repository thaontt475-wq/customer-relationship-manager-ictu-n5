package com.crm.filter;

import com.crm.dao.permissions.MenuDAO;
import com.crm.dto.permissions.MenuItem;
import com.crm.dto.permissions.UserNavigationProfile;
import com.crm.service.permissions.MenuService;
import com.crm.util.SessionKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class MenuNavigationFilterTest {
    private MenuNavigationFilter filter;
    private MenuService menuService;

    @BeforeEach
    void setUp() {
        MenuDAO stubDAO = new MenuDAO() {
            @Override
            public List<String> findRoleNamesByUserId(long userId) {
                if (userId == 200L) return List.of("Sales Rep");
                return List.of();
            }

            @Override
            public UserNavigationProfile findUserNavigationProfile(long userId) {
                if (userId == 200L) {
                    return new UserNavigationProfile(200L, "Nguyễn Văn Tiến", "Nguyễn Văn Tiến", "Sales Rep", "Nhóm Sale 1");
                }
                return null;
            }
        };

        menuService = new MenuService(stubDAO);
        filter = new MenuNavigationFilter(menuService);
    }

    @Test
    @DisplayName("Filter populates menuItems and user navigation profile attributes into request (AC 1 & AC 2)")
    void filterPopulatesRequestAttributesForAuthenticatedUser() throws ServletException, IOException {
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", 200L);
        sessionAttrs.put(SessionKey.ROLES, List.of("Sales Rep"));

        HttpSession session = mockSession(sessionAttrs);
        Map<String, Object> reqAttrs = new HashMap<>();
        HttpServletRequest req = mockRequest(session, reqAttrs, "/dashboard");
        HttpServletResponse resp = mockResponse(new int[1]);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (request, response) -> chainCalled.set(true);

        filter.doFilter(req, resp, chain);

        assertTrue(chainCalled.get(), "FilterChain must proceed");
        assertNotNull(reqAttrs.get("menuItems"), "menuItems attribute should be populated");
        assertNotNull(reqAttrs.get("userNavigationProfile"), "userNavigationProfile should be populated");
        assertEquals("Nguyễn Văn Tiến", reqAttrs.get("currentUserDisplayName"));
        assertEquals("Sales Rep", reqAttrs.get("currentUserRoleLabel"));
        assertEquals("Nhóm Sale 1", reqAttrs.get("currentUserTeamName"));

        @SuppressWarnings("unchecked")
        List<MenuItem> items = (List<MenuItem>) reqAttrs.get("menuItems");
        List<String> codes = items.stream().map(MenuItem::getCode).toList();
        assertTrue(codes.contains("CUSTOMERS"));
        assertFalse(codes.contains("USERS_AUDIT"), "Restricted menu must not be present");
    }

    @Test
    @DisplayName("Filter blocks direct access to restricted admin URL for sales representative with 403 Forbidden")
    void filterBlocksUnauthorizedModuleAccess() throws ServletException, IOException {
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", 200L);
        sessionAttrs.put(SessionKey.ROLES, List.of("Sales Rep"));

        HttpSession session = mockSession(sessionAttrs);
        Map<String, Object> reqAttrs = new HashMap<>();
        HttpServletRequest req = mockRequest(session, reqAttrs, "/users");
        int[] statusHolder = new int[1];
        HttpServletResponse resp = mockResponse(statusHolder);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        FilterChain chain = (request, response) -> chainCalled.set(true);

        filter.doFilter(req, resp, chain);

        assertFalse(chainCalled.get(), "FilterChain should NOT proceed on forbidden access");
        assertEquals(HttpServletResponse.SC_FORBIDDEN, statusHolder[0]);
    }

    private HttpServletRequest mockRequest(HttpSession session, Map<String, Object> attrs, String servletPath) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getServletPath" -> servletPath;
                    case "setAttribute" -> {
                        attrs.put((String) args[0], args[1]);
                        yield null;
                    }
                    case "getAttribute" -> attrs.get((String) args[0]);
                    default -> null;
                });
    }

    private HttpServletResponse mockResponse(int[] statusHolder) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "sendError" -> {
                        statusHolder[0] = (int) args[0];
                        yield null;
                    }
                    case "setStatus" -> {
                        statusHolder[0] = (int) args[0];
                        yield null;
                    }
                    default -> null;
                });
    }

    private HttpSession mockSession(Map<String, Object> attrs) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getAttribute" -> attrs.get((String) args[0]);
                    default -> null;
                });
    }
}
