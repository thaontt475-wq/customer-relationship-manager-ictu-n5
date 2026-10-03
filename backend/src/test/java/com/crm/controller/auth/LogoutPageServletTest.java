package com.crm.controller.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class LogoutPageServletTest {
    @Test
    void postInvalidatesSessionAndRedirectsToLogin() throws Exception {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        var session = mock(HttpSession.class);
        when(req.getSession(false)).thenReturn(session);
        when(req.getContextPath()).thenReturn("");

        new LogoutPageServlet().doPost(req, res);

        verify(session).invalidate();
        verify(res).sendRedirect("/login");
        verify(res).setHeader(eq("Cache-Control"), contains("no-store"));
    }

    @Test
    void postWithoutSessionStillRedirectsToLogin() throws Exception {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        when(req.getSession(false)).thenReturn(null);
        when(req.getContextPath()).thenReturn("/crm");

        new LogoutPageServlet().doPost(req, res);

        verify(res).sendRedirect("/crm/login");
    }

    @Test
    void getNeverInvalidatesSession() throws Exception {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        when(req.getContextPath()).thenReturn("");

        new LogoutPageServlet().doGet(req, res);

        verify(req, never()).getSession(anyBoolean());
        verify(res).sendRedirect("/login");
    }
}
