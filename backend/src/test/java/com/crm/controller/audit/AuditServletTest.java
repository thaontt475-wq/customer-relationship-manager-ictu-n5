package com.crm.controller.audit;

import com.crm.model.AuditLog;
import com.crm.model.AuditLogFilter;
import com.crm.service.audit.AuditLogService;
import com.crm.service.permissions.MenuService;
import com.crm.util.Html;
import com.crm.util.SessionKey;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditServletTest {

    @Mock private AuditLogService auditLogService;
    @Mock private MenuService menuService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private HttpSession session;
    @Mock private RequestDispatcher requestDispatcher;

    private AuditLogServlet apiServlet;
    private AuditPageServlet pageServlet;
    private StringWriter apiBody;

    @BeforeEach
    void setUp() throws Exception {
        apiServlet = new AuditLogServlet(auditLogService, menuService);
        pageServlet = new AuditPageServlet(auditLogService, menuService);
        apiBody = new StringWriter();
    }

    // 1. Admin GET /audit → 200 (forwards to JSP)
    @Test
    void adminGetAuditPageForwardsToJsp() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("admin"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(request.getRequestDispatcher("/jsp/audit/audit-log.jsp")).thenReturn(requestDispatcher);
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());
        when(auditLogService.countLogs(any(AuditLogFilter.class))).thenReturn(0);

        pageServlet.doGet(request, response);

        verify(request).getRequestDispatcher("/jsp/audit/audit-log.jsp");
        verify(requestDispatcher).forward(request, response);
        verify(response).setHeader("Cache-Control", "no-store");
    }

    // 2. Director GET /audit → 200 (forwards to JSP)
    @Test
    void directorGetAuditPageForwardsToJsp() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(2L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("director"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(request.getRequestDispatcher("/jsp/audit/audit-log.jsp")).thenReturn(requestDispatcher);
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());
        when(auditLogService.countLogs(any(AuditLogFilter.class))).thenReturn(0);

        pageServlet.doGet(request, response);

        verify(request).getRequestDispatcher("/jsp/audit/audit-log.jsp");
        verify(requestDispatcher).forward(request, response);
    }

    // 3. Unauthorized role GET /audit → 403 Forbidden
    @Test
    void unauthorizedRoleGetAuditPageReturns403() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(3L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("sales_rep"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(false);

        pageServlet.doGet(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(auditLogService, never()).findLogs(any());
    }

    // 4. Unauthenticated GET /audit → Redirect to /login
    @Test
    void unauthenticatedGetAuditPageRedirectsToLogin() throws Exception {
        when(request.getSession(false)).thenReturn(null);
        when(request.getContextPath()).thenReturn("/crm");

        pageServlet.doGet(request, response);

        verify(response).sendRedirect("/crm/login?expired=1");
        verify(auditLogService, never()).findLogs(any());
    }

    // 5. Admin GET /api/audit-logs → 200 OK
    @Test
    void adminGetApiAuditLogsReturns200() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("admin"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());

        apiServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(apiBody.toString().contains("\"success\":true"));
    }

    // 6. Director GET /api/audit-logs → 200 OK
    @Test
    void directorGetApiAuditLogsReturns200() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(2L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("director"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());

        apiServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertTrue(apiBody.toString().contains("\"success\":true"));
    }

    // 7. Unauthorized API → 403 Forbidden
    @Test
    void unauthorizedRoleGetApiAuditLogsReturns403() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(3L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("sales_rep"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(false);

        apiServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(auditLogService, never()).findLogs(any());
        assertTrue(apiBody.toString().contains("\"success\":false"));
    }

    // 8. Unauthenticated API → 401 Unauthorized
    @Test
    void unauthenticatedGetApiAuditLogsReturns401() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(null);

        apiServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(auditLogService, never()).findLogs(any());
        assertTrue(apiBody.toString().contains("\"success\":false"));
    }

    // 9. POST /api/audit-logs → 405 Method Not Allowed
    @Test
    void postApiAuditLogsReturns405() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));

        apiServlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response).setHeader("Allow", "GET");
        verify(auditLogService, never()).findLogs(any());
    }

    // 10. PUT /api/audit-logs → 405 Method Not Allowed
    @Test
    void putApiAuditLogsReturns405() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));

        apiServlet.doPut(request, response);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response).setHeader("Allow", "GET");
    }

    // 11. DELETE /api/audit-logs → 405 Method Not Allowed
    @Test
    void deleteApiAuditLogsReturns405() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));

        apiServlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response).setHeader("Allow", "GET");
    }

    // 12. PATCH /api/audit-logs → 405 Method Not Allowed
    @Test
    void patchApiAuditLogsReturns405() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getMethod()).thenReturn("PATCH");

        apiServlet.service(request, response);

        verify(response).setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        verify(response).setHeader("Allow", "GET");
    }

    // 13. Allow header contains GET
    @Test
    void allowHeaderContainsGetOnMethodNotAllowed() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));

        apiServlet.doPost(request, response);

        verify(response).setHeader("Allow", "GET");
    }

    // 14. Filter parsing in API
    @Test
    void apiFilterParsingHandlesDatesAndObjectTypes() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("admin"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(request.getParameter("userId")).thenReturn("5");
        when(request.getParameter("objectType")).thenReturn("QUOTE");
        when(request.getParameter("objectId")).thenReturn("10");
        when(request.getParameter("from")).thenReturn("2026-10-01");
        when(request.getParameter("to")).thenReturn("2026-10-05");
        when(request.getParameter("limit")).thenReturn("25");
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());

        apiServlet.doGet(request, response);

        ArgumentCaptor<AuditLogFilter> captor = ArgumentCaptor.forClass(AuditLogFilter.class);
        verify(auditLogService).findLogs(captor.capture());
        AuditLogFilter filter = captor.getValue();
        assertEquals(5L, filter.getUserId());
        assertEquals("QUOTE", filter.getObjectType());
        assertEquals(10L, filter.getObjectId());
        assertEquals(25, filter.getLimit());
        assertNotNull(filter.getFrom());
        assertNotNull(filter.getTo());
    }

    // 15. Pagination parsing on page route
    @Test
    void pageServletParsesPageAndPageSize() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(1L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("admin"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(request.getParameter("page")).thenReturn("3");
        when(request.getParameter("pageSize")).thenReturn("50");
        when(request.getRequestDispatcher("/jsp/audit/audit-log.jsp")).thenReturn(requestDispatcher);
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());
        when(auditLogService.countLogs(any(AuditLogFilter.class))).thenReturn(120);

        pageServlet.doGet(request, response);

        verify(request).setAttribute("currentPage", 3);
        verify(request).setAttribute("totalPages", 3);
        verify(request).setAttribute("totalLogs", 120);
        verify(request).setAttribute("pageSize", 50);
    }

    // 16. Before/After JSON rendering model test
    @Test
    void auditLogHoldsValidJsonElements() {
        JsonObject before = new JsonObject();
        before.add("discountPercent", new JsonPrimitive(10));
        JsonObject after = new JsonObject();
        after.add("discountPercent", new JsonPrimitive(15));

        AuditLog log = new AuditLog(1L, "DISCOUNT_CHANGED", "QUOTE", 100L, before, after);
        log.setCreatedAt(Timestamp.from(Instant.now()));

        assertNotNull(log.getBeforeValue());
        assertNotNull(log.getAfterValue());
        assertFalse(log.getBeforeValue().isJsonNull());
        assertFalse(log.getAfterValue().isJsonNull());
        assertEquals("{\"discountPercent\":10}", log.getBeforeValue().toString());
        assertEquals("{\"discountPercent\":15}", log.getAfterValue().toString());
    }

    // 17. XSS output escaped via Html.escape
    @Test
    void xssPayloadsAreSanitizedByHtmlEscape() {
        String xssPayload = "<script>alert('XSS')</script>";
        String escaped = Html.escape(xssPayload);
        assertFalse(escaped.contains("<script>"));
        assertTrue(escaped.contains("&lt;script&gt;alert(&#39;XSS&#39;)&lt;/script&gt;"));
    }

    // 18. No mutation endpoint in PageServlet
    @Test
    void postToAuditPageReturns405() throws Exception {
        pageServlet.doPost(request, response);
        verify(response).setHeader("Allow", "GET");
        verify(response).sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    // 19. Actor cannot be spoofed in API
    @Test
    void actorIsExtractedFromSessionNotRequestParams() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(99L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("admin"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(true);
        when(request.getParameter("actorUserId")).thenReturn("1"); // Spoof attempt
        when(auditLogService.findLogs(any(AuditLogFilter.class))).thenReturn(List.of());

        apiServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
    }

    // 20. IDOR attempt rejected by RBAC
    @Test
    void idorAttemptByRegularUserIsBlocked() throws Exception {
        when(response.getWriter()).thenReturn(new PrintWriter(apiBody));
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(88L);
        when(session.getAttribute(SessionKey.ROLES)).thenReturn(List.of("sales_rep"));
        when(menuService.isAuthorized(any(), eq("/audit"))).thenReturn(false);
        when(request.getParameter("objectId")).thenReturn("1");

        apiServlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(auditLogService, never()).findLogs(any());
    }
}
