package com.crm.controller.customfields;

import com.crm.model.User;
import com.crm.service.customfields.CustomFieldService;
import com.crm.util.SessionKey;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class CustomFieldPageServletTest {
    private CustomFieldService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private CustomFieldPageServlet servlet;

    @BeforeEach
    void setUp() {
        service = mock(CustomFieldService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        servlet = new CustomFieldPageServlet(service);
        when(request.getContextPath()).thenReturn("");
    }

    @Test
    void doGet_unauthenticated_redirectsToLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/login?expired=1");
        verifyNoInteractions(dispatcher);
    }

    @Test
    void doGet_nonAdminUser_returnsForbidden() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(10L);
        when(session.getAttribute(SessionKey.CURRENT_USER)).thenReturn(new User());
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("SALES"));

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verifyNoInteractions(dispatcher);
    }

    @Test
    void doGet_adminUser_forwardsToJsp() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(10L);
        when(session.getAttribute(SessionKey.CURRENT_USER)).thenReturn(new User());
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("ADMIN"));
        when(request.getRequestDispatcher("/jsp/customfields/custom-field-list.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(dispatcher).forward(request, response);
    }
}
