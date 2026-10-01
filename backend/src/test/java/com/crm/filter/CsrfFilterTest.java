package com.crm.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.mockito.Mockito.*;

class CsrfFilterTest {
    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "DELETE", "PATCH"})
    void blocksAuthenticatedMutationsWithoutToken(String method) throws Exception {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        var session = mock(HttpSession.class);
        var chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("htmlFormToken")).thenReturn("expected");
        new CsrfFilter().doFilter(req, res, chain);
        verify(res).sendError(eq(403), anyString());
        verifyNoInteractions(chain);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void acceptsMatchingHeaderOrFormToken(boolean header) throws Exception {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        var session = mock(HttpSession.class);
        var chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("POST");
        when(req.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute("htmlFormToken")).thenReturn("expected");
        if (header) when(req.getHeader("X-CSRF-Token")).thenReturn("expected");
        else when(req.getParameter("csrfToken")).thenReturn("expected");
        new CsrfFilter().doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }

    @Test void safeRequestsDoNotRequireToken() throws Exception {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        var chain = mock(FilterChain.class);
        when(req.getMethod()).thenReturn("GET");
        new CsrfFilter().doFilter(req, res, chain);
        verify(chain).doFilter(req, res);
    }
}
