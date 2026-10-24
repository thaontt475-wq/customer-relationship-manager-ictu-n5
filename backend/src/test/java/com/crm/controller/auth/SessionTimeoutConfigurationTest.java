package com.crm.controller.auth;

import com.crm.filter.AuthenticationFilter;
import com.crm.util.JsonUtil;
import com.google.gson.JsonObject;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SessionTimeoutConfigurationTest {

    @Test
    @DisplayName("web.xml must configure session-timeout to 15 minutes (900 seconds)")
    void testWebXmlSessionTimeoutConfig() throws Exception {
        Path webXmlPath = Path.of("src/main/webapp/WEB-INF/web.xml");
        if (!Files.exists(webXmlPath)) {
            webXmlPath = Path.of("backend/src/main/webapp/WEB-INF/web.xml");
        }
        assertTrue(Files.exists(webXmlPath), "web.xml must exist at expected path");

        String content = Files.readString(webXmlPath, StandardCharsets.UTF_8);
        assertTrue(content.contains("<session-timeout>15</session-timeout>"),
                "web.xml must contain <session-timeout>15</session-timeout> for 15 minutes inactivity timeout");
    }

    @Test
    @DisplayName("AuthenticationFilter must reject unauthenticated requests with 401 JSON and not create session")
    void testAuthenticationFilterRejectsUnauthenticatedWithoutSession() throws Exception {
        AuthenticationFilter filter = new AuthenticationFilter();

        int[] status = {200};
        StringWriter responseWriter = new StringWriter();
        boolean[] chainCalled = {false};
        boolean[] getSessionTrueCalled = {false};

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("getRequestURI".equals(name)) {
                        return "/crm/api/customers";
                    }
                    if ("getContextPath".equals(name)) {
                        return "/crm";
                    }
                    if ("getMethod".equals(name)) {
                        return "GET";
                    }
                    if ("getSession".equals(name)) {
                        if (args != null && args.length > 0 && Boolean.TRUE.equals(args[0])) {
                            getSessionTrueCalled[0] = true;
                        }
                        return null; // No session active / expired
                    }
                    return null;
                }
        );

        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("setStatus".equals(name)) {
                        status[0] = (int) args[0];
                        return null;
                    }
                    if ("getWriter".equals(name)) {
                        return new PrintWriter(responseWriter);
                    }
                    if ("setCharacterEncoding".equals(name) || "setContentType".equals(name)) {
                        return null;
                    }
                    return null;
                }
        );

        FilterChain chain = (request, response) -> chainCalled[0] = true;

        filter.doFilter(req, res, chain);

        assertFalse(chainCalled[0], "FilterChain should not be called when unauthenticated");
        assertFalse(getSessionTrueCalled[0], "Filter must not call getSession(true) to create new session");
        assertEquals(401, status[0], "Response status must be HTTP 401");

        String jsonText = responseWriter.toString().trim();
        JsonObject json = JsonUtil.getGson().fromJson(jsonText, JsonObject.class);
        assertNotNull(json, "Response body must be valid JSON");
        assertFalse(json.get("success").getAsBoolean(), "success must be false");
        assertEquals("Phiên đăng nhập đã hết hạn hoặc chưa đăng nhập. Vui lòng đăng nhập lại.", json.get("message").getAsString(), "message must indicate unauthenticated or session expired");
    }

    @Test
    @DisplayName("LogoutServlet must invalidate session and return success")
    void testLogoutServletInvalidatesSession() throws Exception {
        LogoutServlet servlet = new LogoutServlet();

        boolean[] invalidated = {false};
        HttpSession mockSession = (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpSession.class},
                (proxy, method, args) -> {
                    if ("invalidate".equals(method.getName())) {
                        invalidated[0] = true;
                    }
                    return null;
                }
        );

        int[] status = {200};
        StringWriter responseWriter = new StringWriter();

        HttpServletRequest req = (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    if ("getSession".equals(method.getName())) {
                        return mockSession;
                    }
                    return null;
                }
        );

        HttpServletResponse res = (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if ("setStatus".equals(method.getName())) {
                        status[0] = (int) args[0];
                    }
                    if ("getWriter".equals(method.getName())) {
                        return new PrintWriter(responseWriter);
                    }
                    return null;
                }
        );

        servlet.doPost(req, res);

        assertTrue(invalidated[0], "LogoutServlet must invalidate existing session");
        assertEquals(200, status[0], "LogoutServlet must return 200 OK");
        JsonObject json = JsonUtil.getGson().fromJson(responseWriter.toString(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean(), "Logout response success must be true");
    }

    @Test
    @DisplayName("Session metadata must expose sessionTimeoutSeconds = 900 (15 minutes)")
    void testSessionMetadataExposesTimeoutSeconds() {
        Map<String, Object> account = new HashMap<>();
        account.put("id", 1L);
        account.put("username", "admin");
        account.put("fullName", "System Admin");
        account.put("roleName", "ADMIN");

        Map<String, Object> payload = SessionServlet.describeWithTimeout(account);
        assertNotNull(payload, "Payload must not be null");
        assertEquals(900, payload.get("sessionTimeoutSeconds"), "Session timeout must be 900 seconds (15m)");
        assertEquals("ADMIN", payload.get("roleName"));
    }
}
