package com.crm.controller.winloss;

import com.crm.model.Competitor;
import com.crm.model.Role;
import com.crm.model.User;
import com.crm.model.WinLossReason;
import com.crm.service.winloss.WinLossService;
import com.crm.service.winloss.WinLossService.DeleteOutcome;
import com.crm.service.winloss.WinLossService.DuplicateException;
import com.crm.service.winloss.WinLossService.NotFoundException;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/** API contract used by the merged CRM-48 frontend. */
@WebServlet({"/api/winloss/reasons", "/api/winloss/competitors"})
public class WinLossServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(WinLossServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();
    private static final Set<String> CONFIG_ROLES = Set.of(
            "ADMIN", "ADMINISTRATOR", "SYSTEM_ADMIN", "DIRECTOR",
            "GIÁM ĐỐC", "GIAM DOC", "QUẢN TRỊ VIÊN", "QUAN TRI VIEN");

    private final WinLossService service;

    public WinLossServlet() { this(new WinLossService()); }
    public WinLossServlet(WinLossService service) { this.service = service; }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!isAuthenticated(request)) {
            writeJson(response, 401, false, "Yêu cầu đăng nhập", null);
            return;
        }
        boolean includeInactive = "true".equalsIgnoreCase(request.getParameter("includeInactive"));
        try {
            if (isCompetitorRequest(request)) {
                writeJson(response, 200, true, "Lấy danh sách đối thủ thành công",
                        service.getCompetitors(includeInactive));
            } else {
                writeJson(response, 200, true, "Lấy danh mục lý do thắng/thua thành công",
                        service.getReasons(includeInactive));
            }
        } catch (Exception e) {
            handleException(response, "tải dữ liệu thắng/thua", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!requireAdmin(request, response)) return;
        try {
            if (isCompetitorRequest(request)) {
                Competitor competitor = readJson(request, Competitor.class);
                writeJson(response, 201, true, "Thêm đối thủ thành công",
                        service.createCompetitor(competitor));
            } else {
                WinLossReason reason = readJson(request, WinLossReason.class);
                writeJson(response, 201, true, "Thêm lý do thành công", service.createReason(reason));
            }
        } catch (Exception e) {
            handleException(response, "tạo dữ liệu thắng/thua", e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!requireAdmin(request, response)) return;
        try {
            if (isCompetitorRequest(request)) {
                Competitor competitor = readJson(request, Competitor.class);
                writeJson(response, 200, true, "Cập nhật đối thủ thành công",
                        service.updateCompetitor(competitor));
            } else {
                WinLossReason reason = readJson(request, WinLossReason.class);
                writeJson(response, 200, true, "Cập nhật lý do thành công", service.updateReason(reason));
            }
        } catch (Exception e) {
            handleException(response, "cập nhật dữ liệu thắng/thua", e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        prepare(request);
        if (!requireAdmin(request, response)) return;
        try {
            long id = parseId(request.getParameter("id"));
            DeleteOutcome outcome = isCompetitorRequest(request)
                    ? service.deleteCompetitor(id) : service.deleteReason(id);
            String message = outcome == DeleteOutcome.DEACTIVATED
                    ? "Dữ liệu đang được tham chiếu nên đã chuyển sang ngừng kích hoạt"
                    : "Xóa dữ liệu thành công";
            writeJson(response, 200, true, message, Map.of("outcome", outcome.name()));
        } catch (Exception e) {
            handleException(response, "xóa dữ liệu thắng/thua", e);
        }
    }

    private boolean requireAdmin(HttpServletRequest request, HttpServletResponse response) throws IOException {
        if (!isAuthenticated(request)) {
            writeJson(response, 401, false, "Yêu cầu đăng nhập", null);
            return false;
        }
        if (!hasAdminRole(request.getSession(false))) {
            writeJson(response, 403, false, "Chỉ Admin/Director được cấu hình dữ liệu thắng/thua", null);
            return false;
        }
        return true;
    }

    private boolean isAuthenticated(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return false;
        Object directId = session.getAttribute("userId");
        if (directId instanceof Number number && number.longValue() > 0) return true;
        if (directId instanceof String text) {
            try { if (Long.parseLong(text) > 0) return true; } catch (NumberFormatException ignored) { }
        }
        Object current = session.getAttribute(SessionKey.CURRENT_USER);
        if (current instanceof User user) return user.getId() > 0;
        if (current instanceof Map<?, ?> map) {
            Object id = map.get("id");
            return id instanceof Number number && number.longValue() > 0;
        }
        return false;
    }

    private boolean hasAdminRole(HttpSession session) {
        if (session == null) return false;
        List<String> roles = new ArrayList<>();
        collectRoles(session.getAttribute(SessionKey.ROLES), roles);
        Object current = session.getAttribute(SessionKey.CURRENT_USER);
        if (current instanceof User user) {
            collectRoles(user.getRoles(), roles);
            collectRoles(user.getRole(), roles);
        } else if (current instanceof Map<?, ?> map) {
            collectRoles(map.get("roles"), roles);
            collectRoles(map.get("role"), roles);
        }
        return roles.stream().map(this::normalizeRole).anyMatch(CONFIG_ROLES::contains);
    }

    private void collectRoles(Object value, List<String> result) {
        if (value == null) return;
        if (value instanceof Collection<?> collection) {
            collection.forEach(item -> collectRoles(item, result));
        } else if (value instanceof Role role) {
            result.add(role.getName());
        } else if (value instanceof Map<?, ?> map && map.get("name") != null) {
            result.add(String.valueOf(map.get("name")));
        } else {
            result.add(String.valueOf(value));
        }
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        return normalized.startsWith("ROLE_") ? normalized.substring(5) : normalized;
    }

    private boolean isCompetitorRequest(HttpServletRequest request) {
        return request.getServletPath() != null && request.getServletPath().endsWith("/competitors");
    }

    private long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (Exception e) {
            throw new IllegalArgumentException("ID không hợp lệ");
        }
    }

    private <T> T readJson(HttpServletRequest request, Class<T> type) throws IOException {
        T value = GSON.fromJson(request.getReader(), type);
        if (value == null) throw new IllegalArgumentException("Body JSON không hợp lệ");
        return value;
    }

    private void handleException(HttpServletResponse response, String operation, Exception error)
            throws IOException {
        if (error instanceof NotFoundException) {
            writeJson(response, 404, false, error.getMessage(), null);
        } else if (error instanceof DuplicateException) {
            writeJson(response, 409, false, error.getMessage(), null);
        } else if (error instanceof IllegalArgumentException || error instanceof JsonParseException) {
            writeJson(response, 400, false, error.getMessage(), null);
        } else if (error instanceof SQLException) {
            LOGGER.log(Level.SEVERE, "CRM-48 database error while " + operation, error);
            writeJson(response, 500, false, "Lỗi hệ thống khi " + operation, null);
        } else {
            LOGGER.log(Level.SEVERE, "CRM-48 unexpected error while " + operation, error);
            writeJson(response, 500, false, "Lỗi hệ thống khi " + operation, null);
        }
    }

    private void prepare(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding(StandardCharsets.UTF_8.name());
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
