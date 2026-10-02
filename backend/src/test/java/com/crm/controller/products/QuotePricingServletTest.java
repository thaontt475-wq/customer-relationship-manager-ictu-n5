package com.crm.controller.products;

import com.crm.model.User;
import com.crm.service.products.QuotePricingService;
import com.crm.util.SessionKey;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class QuotePricingServletTest {
    private QuotePricingService service;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;
    private RequestDispatcher dispatcher;
    private QuotePricingServlet servlet;

    @BeforeEach
    void setUp() {
        service = mock(QuotePricingService.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        dispatcher = mock(RequestDispatcher.class);
        servlet = new QuotePricingServlet(service);
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
    @DisplayName("GET /quotes/pricing unauthenticated redirects to login")
    void doGet_unauthenticated_redirectsToLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).sendRedirect("/login?expired=1");
        verifyNoInteractions(dispatcher);
    }

    @Test
    @DisplayName("GET /quotes/pricing with valid id forwards to JSP")
    void doGet_validId_forwardsToJsp() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getParameter("id")).thenReturn("5");
        when(request.getRequestDispatcher("/jsp/products/quote-pricing.jsp")).thenReturn(dispatcher);
        when(service.read(10L, 5L)).thenReturn(new QuotePricingService.Pricing(List.of(), BigDecimal.ZERO, BigDecimal.ZERO, false, "DRAFT"));

        servlet.doGet(request, response);

        verify(request).setAttribute("quoteId", 5L);
        verify(dispatcher).forward(request, response);
    }

    @Test
    @DisplayName("GET /quotes/pricing with invalid id returns HTTP 400")
    void doGet_invalidId_returns400() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getParameter("id")).thenReturn("abc");

        servlet.doGet(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
    }

    @Test
    @DisplayName("POST /quotes/approve by admin calls approve and redirects")
    void doPost_approve_byAdmin_redirects() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/approve");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("5");

        servlet.doPost(request, response);

        verify(service).approve(10L, 5L);
        verify(response).sendRedirect("/quotes/pricing?id=5");
    }

    @Test
    @DisplayName("POST /quotes/items adds item and redirects")
    void doPost_items_valid_redirects() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/items");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("5");
        when(request.getParameter("productId")).thenReturn("12");
        when(request.getParameter("quantity")).thenReturn("2.00");
        when(request.getParameter("unitPrice")).thenReturn("5000000.00");

        servlet.doPost(request, response);

        verify(service).addItem(10L, 5L, 12L, new BigDecimal("2.00"), new BigDecimal("5000000.00"));
        verify(response).sendRedirect("/quotes/pricing?id=5");
    }

    @Test
    @DisplayName("POST /quotes/items with invalid numbers returns HTTP 400")
    void doPost_items_invalid_returns400() throws Exception {
        mockSession(10L, List.of("Sales Rep"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/items");
        when(request.getParameter("csrfToken")).thenReturn("token123");
        when(request.getParameter("id")).thenReturn("5");
        when(request.getParameter("productId")).thenReturn("12");
        when(request.getParameter("quantity")).thenReturn("not-a-number");
        when(request.getParameter("unitPrice")).thenReturn("5000000.00");

        servlet.doPost(request, response);

        verify(response).sendError(eq(HttpServletResponse.SC_BAD_REQUEST), any());
        verify(service, never()).addItem(anyLong(), anyLong(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("POST with invalid CSRF token returns HTTP 403")
    void doPost_invalidCsrf_returnsForbidden() throws Exception {
        mockSession(10L, List.of("Admin"), "token123");
        when(request.getServletPath()).thenReturn("/quotes/approve");
        when(request.getParameter("csrfToken")).thenReturn("wrong_token");

        servlet.doPost(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(service, never()).approve(anyLong(), anyLong());
    }
}
