package com.crm.controller.audit;

import com.crm.model.AuditLogFilter;
import com.crm.service.permissions.MenuService;
import java.util.ArrayList;
import java.util.List;
import com.crm.service.audit.AuditLogService;
import com.crm.util.SessionKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Browser-rendered read-only audit page. No client-side scripting is required. */
@WebServlet("/audit")
public class AuditPageServlet extends HttpServlet {
    private static final Logger LOG = Logger.getLogger(AuditPageServlet.class.getName());
    private final AuditLogService service;

    public AuditPageServlet() { this(new AuditLogService()); }
    public AuditPageServlet(AuditLogService service) { this.service = service; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !signedIn(session)) {
            response.sendRedirect(request.getContextPath() + "/login?expired=1");
            return;
        }
        // MenuNavigationFilter guards /audit with the existing module permission.
        // Restrict direct access here as well when the filter is accidentally unmapped.
        if (!new MenuService().isAuthorized(sessionRoles(session), "/audit")) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        response.setHeader("Cache-Control", "no-store");
        try {
            AuditLogFilter filter = new AuditLogFilter();
            filter.setUserId(positiveLong(request.getParameter("userId")));
            filter.setObjectId(positiveLong(request.getParameter("objectId")));
            String type = trim(request.getParameter("objectType"));
            filter.setObjectType(type.isEmpty() ? null : type);
            String action = trim(request.getParameter("action"));
            filter.setAction(action.isEmpty() ? null : action);
            filter.setFrom(date(request.getParameter("from"), false));
            filter.setTo(date(request.getParameter("to"), true));
            filter.setLimit(50); // Set smaller limit for pages
            
            String pageStr = request.getParameter("page");
            int page = 1;
            if (pageStr != null && !pageStr.isBlank()) {
                try {
                    page = Integer.parseInt(pageStr);
                    if (page < 1) page = 1;
                } catch (NumberFormatException ignored) {}
            }
            filter.setPage(page);

            request.setAttribute("auditLogs", service.findLogs(filter));
            int totalLogs = service.countLogs(filter);
            int totalPages = (int) Math.ceil((double) totalLogs / filter.getLimit());
            request.setAttribute("currentPage", filter.getPage());
            request.setAttribute("totalPages", totalPages);
        } catch (IllegalArgumentException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            request.setAttribute("auditError", e.getMessage());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Unable to render audit page", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            request.setAttribute("auditError", "Không thể tải nhật ký. Vui lòng thử lại sau.");
        }
        request.getRequestDispatcher("/jsp/audit/audit-log.jsp").forward(request, response);
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
    private static Long positiveLong(String s) {
        if (trim(s).isEmpty()) return null;
        try {
            long value = Long.parseLong(s.trim());
            if (value > 0) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException("Mã người dùng hoặc bản ghi phải là số nguyên dương.");
    }
    private static Timestamp date(String s, boolean end) {
        if (trim(s).isEmpty()) return null;
        try {
            LocalDate day = LocalDate.parse(s.trim());
            return Timestamp.valueOf(end ? day.atTime(LocalTime.MAX) : day.atStartOfDay());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Ngày lọc không hợp lệ (yyyy-MM-dd).");
        }
    }
    private static boolean signedIn(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (id instanceof Number n && n.longValue() > 0) return true;
        Object user = session.getAttribute(SessionKey.CURRENT_USER);
        if (user instanceof com.crm.model.User u && u.getId() > 0) return true;
        if (user instanceof Map<?, ?> m && m.get("id") instanceof Number n && n.longValue() > 0) return true;
        return false;
    }
    private static List<String> sessionRoles(HttpSession session) {
        Object roles = session.getAttribute(SessionKey.ROLES);
        if (roles == null) roles = session.getAttribute("roles");
        List<String> result = new ArrayList<>();
        if (roles instanceof Iterable<?> iterable) {
            for (Object role : iterable) {
                if (role != null) result.add(String.valueOf(role));
            }
        }
        return result;
    }
}
