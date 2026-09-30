package com.crm.controller.permissions;

import com.crm.dao.permissions.MenuDAO;
import com.crm.dto.permissions.UserNavigationProfile;
import com.crm.service.permissions.MenuService;
import com.crm.util.SessionKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuServletTest {
    private MenuService menuService;
    private MenuServlet menuServlet;

    @BeforeEach
    void setUp() {
        // Stub MenuDAO for isolated testing
        MenuDAO stubDAO = new MenuDAO() {
            @Override
            public List<String> findRoleNamesByUserId(long userId) {
                if (userId == 1L) return List.of("Admin");
                if (userId == 2L) return List.of("Sales Rep");
                return List.of();
            }

            @Override
            public UserNavigationProfile findUserNavigationProfile(long userId) {
                if (userId == 1L) {
                    return new UserNavigationProfile(1L, "Admin User", "Administrator", "Admin", "Ban Giám Đốc");
                }
                if (userId == 2L) {
                    return new UserNavigationProfile(2L, "Tiến Sales", "Nguyễn Tiến", "Sales Rep", "Nhóm Kinh Doanh 1");
                }
                return null;
            }
        };

        menuService = new MenuService(stubDAO);
        menuServlet = new MenuServlet(menuService);
    }

    @Test
    @DisplayName("MenuServlet returns 401 when no session exists")
    void unauthenticatedRequestReturns401() throws IOException {
        HttpServletRequest req = mockRequest(null);
        StringWriter sw = new StringWriter();
        int[] statusHolder = new int[1];
        HttpServletResponse resp = mockResponse(sw, statusHolder);

        menuServlet.doGet(req, resp);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, statusHolder[0]);
        assertTrue(sw.toString().contains("\"success\":false"));
        assertTrue(sw.toString().contains("Chưa đăng nhập"));
    }

    @Test
    @DisplayName("MenuServlet returns role-filtered menu and user profile (AC 1 & AC 2)")
    void authenticatedRequestReturnsRoleBasedMenuAndUserProfile() throws IOException {
        Map<String, Object> sessionAttrs = new HashMap<>();
        sessionAttrs.put("userId", 2L);
        sessionAttrs.put(SessionKey.ROLES, List.of("Sales Rep"));

        HttpSession session = mockSession(sessionAttrs);
        HttpServletRequest req = mockRequest(session);
        StringWriter sw = new StringWriter();
        int[] statusHolder = new int[1];
        HttpServletResponse resp = mockResponse(sw, statusHolder);

        menuServlet.doGet(req, resp);

        assertEquals(HttpServletResponse.SC_OK, statusHolder[0]);
        String json = sw.toString();
        assertTrue(json.contains("\"success\":true"));

        // AC 1: Sales Rep menu contains CUSTOMERS and not USERS_AUDIT
        assertTrue(json.contains("CUSTOMERS"));
        assertTrue(!json.contains("USERS_AUDIT"));

        // AC 2: User profile with name, role and team
        assertTrue(json.contains("Tiến Sales"));
        assertTrue(json.contains("Sales Rep"));
        assertTrue(json.contains("Nhóm Kinh Doanh 1"));
    }

    private HttpServletRequest mockRequest(HttpSession session) {
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getSession" -> session;
                    case "getHeader" -> null;
                    case "getContentType" -> "application/json";
                    default -> null;
                });
    }

    private HttpServletResponse mockResponse(StringWriter stringWriter, int[] statusHolder) {
        PrintWriter pw = new PrintWriter(stringWriter);
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setStatus" -> {
                        statusHolder[0] = (int) args[0];
                        yield null;
                    }
                    case "getWriter" -> pw;
                    case "setContentType", "setCharacterEncoding" -> null;
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
