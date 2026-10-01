package com.crm.controller.auth;

import com.crm.service.auth.AuthService;
import com.crm.util.SessionKey;
import com.crm.util.SessionRegistry;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginServletTest {
    private static final String GENERIC_LOGIN_ERROR = "Email hoặc mật khẩu không đúng";

    private AuthService authService;
    private LoginServlet servlet;
    private HttpSession registeredSession;

    @BeforeEach
    void setUp() throws Exception {
        authService = mock(AuthService.class);
        servlet = new LoginServlet();
        inject(servlet, "authService", authService);
    }

    @AfterEach
    void cleanUpSessionRegistry() {
        if (registeredSession != null) {
            SessionRegistry.unregister(registeredSession);
        }
    }

    @Test
    void unknownEmailAndWrongPasswordUseTheSameGenericMessage() throws Exception {
        assertRejectedWithGenericMessage("missing@example.test", "AnyPassword1!");
        assertRejectedWithGenericMessage("known@example.test", "WrongPassword1!");
    }

    @Test
    void successfulLoginStoresRolesAndRedirectsOnlyToDashboard() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        AuthService.LoginResult result = new AuthService.LoginResult(
                2101L, "CRM Admin", List.of("Admin"));

        when(request.getParameter("email")).thenReturn("admin@example.test");
        when(request.getParameter("password")).thenReturn("CorrectPassword1!");
        when(request.getSession(false)).thenReturn(null);
        when(request.getSession(true)).thenReturn(session);
        when(request.getContextPath()).thenReturn("/crm");
        when(session.getId()).thenReturn("crm-21-login-session");
        when(authService.login("admin@example.test", "CorrectPassword1!")).thenReturn(result);
        registeredSession = session;

        servlet.doPost(request, response);

        verify(session).setAttribute("userId", 2101L);
        verify(session).setAttribute(SessionKey.ROLES, List.of("Admin"));
        verify(response).sendRedirect("/crm/dashboard");
    }

    private void assertRejectedWithGenericMessage(String email, String password) throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        RequestDispatcher dispatcher = mock(RequestDispatcher.class);

        when(request.getParameter("email")).thenReturn(email);
        when(request.getParameter("password")).thenReturn(password);
        when(request.getRequestDispatcher("/jsp/auth/login.jsp")).thenReturn(dispatcher);
        when(authService.login(email, password)).thenReturn(null);

        servlet.doPost(request, response);

        verify(request).setAttribute("error", GENERIC_LOGIN_ERROR);
        verify(dispatcher).forward(request, response);
    }

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
