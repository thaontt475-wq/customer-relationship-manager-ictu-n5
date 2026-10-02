package com.crm.controller.audit;

import com.crm.model.User;
import com.crm.service.audit.BusinessChangeService;
import com.crm.service.users.UserService;
import com.crm.util.SessionKey;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BusinessChangeServletTest {
    private BusinessChangeService service;
    private UserService userService;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private BusinessChangeServlet servlet;

    @BeforeEach
    void setUp() {
        service = mock(BusinessChangeService.class);
        userService = mock(UserService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        servlet = new BusinessChangeServlet(service, userService);
        when(request.getContextPath()).thenReturn("");
    }

    private void mockSession(long userId, List<String> roles, String csrfToken) {
        when(request.getSession(false)).thenReturn(session);
        when(request.getSession()).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(userId);
        when(session.getAttribute(SessionKey.CURRENT_USER)).thenReturn(new User());
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(roles);
        when(session.getAttribute("htmlFormToken")).thenReturn(csrfToken);
    }

    @Test
    @DisplayName("GET /kpi unauthenticated redirects to login")
    void doGet_kpi_unauthenticated_redirectsToLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getServletPath()).thenReturn("/kpi");

        servlet.doGet(request, response);

        verify(response).sendRedirect("/login?expired=1");
        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("GET /kpi by non-admin returns HTTP 403")
    void doGet_kpi_nonAdminUser_returnsForbidden() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("GET /kpi by Admin forwards to JSP")
    void doGet_kpi_adminUser_forwardsToJsp() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");
        when(request.getRequestDispatcher("/jsp/audit/sales-targets.jsp")).thenReturn(dispatcher);

        servlet.doGet(request, response);

        verify(service).targets(10L);
        verify(userService).findAll();
        verify(dispatcher).forward(request, response);
    }

    @Test
    @DisplayName("GET /quotes/discount returns 405 Method Not Allowed")
    void doGet_quotesDiscount_returns405MethodNotAllowed() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/discount");

        servlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("POST /kpi with valid data sets target and redirects")
    void doPost_kpi_validTarget_redirectsWithSavedParam() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("userId")).thenReturn("20");
        when(request.getParameter("month")).thenReturn("2026-10");
        when(request.getParameter("amount")).thenReturn("50000000.00");

        servlet.doPost(request, response);

        verify(service).setTarget(10L, 20L, LocalDate.of(2026, 10, 1), new BigDecimal("50000000.00"));
        verify(response).sendRedirect("/kpi?saved=1");
    }

    @Test
    @DisplayName("POST /kpi with missing params returns HTTP 400")
    void doPost_kpi_missingParams_returns400() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("userId")).thenReturn("20");
        when(request.getParameter("month")).thenReturn("");
        when(request.getParameter("amount")).thenReturn("50000000");

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
        verify(service, never()).setTarget(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /kpi with invalid amount returns HTTP 400")
    void doPost_kpi_invalidAmount_returns400() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("userId")).thenReturn("20");
        when(request.getParameter("month")).thenReturn("2026-10");
        when(request.getParameter("amount")).thenReturn("not-a-number");

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
        verify(service, never()).setTarget(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /kpi with non-admin user returns HTTP 403")
    void doPost_kpi_nonAdmin_returnsForbidden() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");
        when(request.getParameter("csrfToken")).thenReturn("token123");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(service, never()).setTarget(anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST /quotes/discount with valid discount changes discount and redirects")
    void doPost_quotesDiscount_validDiscount_redirectsToQuote() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/discount");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("100");
        when(request.getParameter("discount")).thenReturn("15.50");

        servlet.doPost(request, response);

        verify(service).changeDiscount(10L, 100L, new BigDecimal("15.50"));
        verify(response).sendRedirect("/quotes?id=100");
    }

    @Test
    @DisplayName("POST /quotes/discount with missing params returns HTTP 400")
    void doPost_quotesDiscount_missingParams_returns400() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/discount");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("100");
        when(request.getParameter("discount")).thenReturn("");

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
        verify(service, never()).changeDiscount(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("POST /quotes/discount with invalid number returns HTTP 400")
    void doPost_quotesDiscount_invalidNumber_returns400() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/discount");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("100");
        when(request.getParameter("discount")).thenReturn("invalid_discount");

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
        verify(service, never()).changeDiscount(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("POST /quotes/discount with unauthorized scope returns HTTP 403")
    void doPost_quotesDiscount_permissionDenied_returnsForbidden() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/discount");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("100");
        when(request.getParameter("discount")).thenReturn("15.00");

        doThrow(new SecurityException("Bạn không có quyền")).when(service).changeDiscount(10L, 100L, new BigDecimal("15.00"));

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_FORBIDDEN), any());
    }

    @Test
    @DisplayName("POST with invalid CSRF token returns HTTP 403")
    void doPost_invalidCsrf_returnsForbidden() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/kpi");
        when(request.getParameter("csrfToken")).thenReturn("wrong_token");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(service, never()).setTarget(anyLong(), anyLong(), any(), any());
    }
}
