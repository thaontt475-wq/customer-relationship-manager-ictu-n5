package com.crm.controller.audit;

import com.crm.model.AuditLog;
import com.crm.model.AuditLogFilter;
import com.crm.model.User;
import com.crm.service.audit.AuditLogService;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Read-only CRM-37 API. Audit records are created by business services, never by clients.
 */
@WebServlet("/api/audit-logs")
public class AuditLogServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(AuditLogServlet.class.getName());
    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
            .create();

    private final AuditLogService auditLogService;

    public AuditLogServlet() {
        this(new AuditLogService());
    }

    public AuditLogServlet(AuditLogService auditLogService) {
        this.auditLogService = auditLogService != null ? auditLogService : new AuditLogService();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (extractActorUserId(request) == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false, "Yêu cầu đăng nhập", null);
            return;
        }

        try {
            AuditLogFilter filter = parseFilter(request);
            List<AuditLog> logs = auditLogService.findLogs(filter);
            writeJson(response, HttpServletResponse.SC_OK, true,
                    "Lấy nhật ký thay đổi thành công", logs);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-37: Database error loading audit logs", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tải nhật ký thay đổi", null);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeMethodNotAllowed(response);
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeMethodNotAllowed(response);
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        writeMethodNotAllowed(response);
    }

    private AuditLogFilter parseFilter(HttpServletRequest request) {
        AuditLogFilter filter = new AuditLogFilter();
        filter.setUserId(parsePositiveLong(request.getParameter("userId"), "userId"));
        filter.setObjectId(parsePositiveLong(request.getParameter("objectId"), "objectId"));

        String objectType = request.getParameter("objectType");
        filter.setObjectType(objectType == null || objectType.isBlank() ? null : objectType.trim());
        filter.setFrom(parseTimestamp(request.getParameter("from"), "from", false));
        filter.setTo(parseTimestamp(request.getParameter("to"), "to", true));

        String limit = request.getParameter("limit");
        if (limit != null && !limit.isBlank()) {
            try {
                filter.setLimit(Integer.parseInt(limit.trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("limit phải là số nguyên");
            }
        }
        return filter;
    }

    private Long parsePositiveLong(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            if (parsed <= 0) {
                throw new IllegalArgumentException(name + " phải là số dương");
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + " phải là số dương");
        }
    }

    private Timestamp parseTimestamp(String value, String name, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
                LocalDate date = LocalDate.parse(normalized);
                LocalDateTime dateTime = endOfDay
                        ? date.atTime(LocalTime.MAX)
                        : date.atStartOfDay();
                return Timestamp.valueOf(dateTime);
            }
            try {
                return Timestamp.from(Instant.parse(normalized));
            } catch (DateTimeParseException ignored) {
                // Continue with offset or local ISO date-time formats.
            }
            try {
                return Timestamp.from(OffsetDateTime.parse(normalized).toInstant());
            } catch (DateTimeParseException ignored) {
                return Timestamp.valueOf(LocalDateTime.parse(normalized));
            }
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new IllegalArgumentException(name
                    + " phải theo ISO-8601, ví dụ 2026-10-01 hoặc 2026-10-01T08:30:00Z");
        }
    }

    private Long extractActorUserId(HttpServletRequest request) {
        HttpSession session;
        try {
            session = request.getSession(false);
        } catch (IllegalStateException e) {
            return null;
        }
        if (session == null) {
            return null;
        }

        Object directUserId = session.getAttribute("userId");
        Long parsed = parseSessionId(directUserId);
        if (parsed != null) {
            return parsed;
        }
        Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        if (currentUser instanceof User user && user.getId() > 0) {
            return user.getId();
        }
        if (currentUser instanceof Map<?, ?> map) {
            return parseSessionId(map.get("id"));
        }
        return parseSessionId(currentUser);
    }

    private Long parseSessionId(Object value) {
        if (value instanceof Number number) {
            return number.longValue() > 0 ? number.longValue() : null;
        }
        if (value instanceof String text) {
            try {
                long id = Long.parseLong(text.trim());
                return id > 0 ? id : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void writeMethodNotAllowed(HttpServletResponse response) throws IOException {
        response.setHeader("Allow", "GET");
        writeJson(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, false,
                "Audit log là dữ liệu chỉ đọc; client không được phép tạo hoặc chỉnh sửa", null);
    }

    private void writeJson(HttpServletResponse response, int status, boolean success,
                           String message, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        GSON.toJson(new ApiResponse(success, message, data), response.getWriter());
    }

    private record ApiResponse(boolean success, String message, Object data) { }

}
