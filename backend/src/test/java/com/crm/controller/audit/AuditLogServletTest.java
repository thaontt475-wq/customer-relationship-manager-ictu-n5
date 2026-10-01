package com.crm.controller.audit;

import com.crm.model.AuditLogFilter;
import com.crm.service.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServletTest {
    @Mock private AuditLogService auditLogService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;

    private AuditLogServlet servlet;
    private StringWriter body;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new AuditLogServlet(auditLogService);
        body = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(body));
    }

    @Test
    void getRequiresAuthenticatedSession() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(auditLogService, never()).findLogs(any());
        assertTrue(body.toString().contains("\"success\":false"));
    }

    @Test
    void getParsesAllSupportedFilters() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(5L);
        when(request.getParameter("userId")).thenReturn("7");
        when(request.getParameter("objectType")).thenReturn("opportunity");
        when(request.getParameter("objectId")).thenReturn("12");
        when(request.getParameter("from")).thenReturn("2026-10-01");
        when(request.getParameter("to")).thenReturn("2026-10-02T12:30:00");
        when(request.getParameter("limit")).thenReturn("50");
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());

        servlet.doGet(request, response);

        ArgumentCaptor<AuditLogFilter> captor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(auditLogService).findLogs(captor.capture());
        AuditLogFilter filter = captor.getValue();
        assertEquals(7L, filter.getUserId());
        assertEquals("opportunity", filter.getObjectType());
        assertEquals(12L, filter.getObjectId());
        assertEquals(50, filter.getLimit());
        assertEquals("2026-10-01 00:00:00.0", filter.getFrom().toString());
        assertEquals("2026-10-02 12:30:00.0", filter.getTo().toString());
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(body.toString().contains("\"success\":true"));
    }

    @Test
    void postIsRejectedBecauseAuditCreationIsInternalOnly() throws Exception {
        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response).setHeader("Allow", "GET");
        verify(auditLogService, never()).findLogs(any());
    }
}
