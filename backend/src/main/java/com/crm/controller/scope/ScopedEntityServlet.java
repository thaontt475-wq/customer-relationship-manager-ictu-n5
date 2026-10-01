package com.crm.controller.scope;

import com.crm.model.User;
import com.crm.service.scope.DataScopeService;
import com.crm.service.scope.ScopeEntityType;
import com.crm.service.scope.ScopeRecord;
import com.crm.util.SessionKey;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Servlet handling Scoped Entity access for the 4 core domain modules (CRM-25):
 * - Customers      (/api/customers)
 * - Opportunities  (/api/opportunities)
 * - Activities     (/api/activities)
 * - Quotes         (/api/quotes)
 *
 * Enforces Data Scope (SELF / TEAM / ALL) across:
 * 1. List      (GET /api/{entity})
 * 2. Search    (GET /api/{entity}?search=... or ?q=...)
 * 3. Detail    (GET /api/{entity}/{id}) — returns 403 if record is outside user's scope
 * 4. Export    (GET /api/{entity}/export or ?export=true) — exports only scoped records
 * 5. Update    (PUT /api/{entity}/{id}) — returns 403 if record is outside user's scope
 * 6. Delete    (DELETE /api/{entity}/{id}) — returns 403 if record is outside user's scope
 */
@WebServlet({
        "/api/customers",
        "/api/customers/*",
        "/api/opportunities",
        "/api/opportunities/*",
        "/api/activities",
        "/api/activities/*",
        "/api/quotes",
        "/api/quotes/*"
})
public class ScopedEntityServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(ScopedEntityServlet.class.getName());
    private static final Gson GSON = new GsonBuilder().serializeNulls().create();

    private final DataScopeService dataScopeService;

    public ScopedEntityServlet() {
        this.dataScopeService = new DataScopeService();
    }

    public ScopedEntityServlet(DataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService != null ? dataScopeService : new DataScopeService();
    }

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long userId = currentUserId(request);
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        ScopeEntityType type = resolveEntityType(request);
        if (type == null) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài nguyên", null);
            return;
        }

        String pathInfo = request.getPathInfo();
        String searchQuery = extractSearchQuery(request);
        boolean isExport = isExportRequest(request, pathInfo);

        try {
            // Export uses exactly the same scope and search as the list.
            if (isExport) {
                List<ScopeRecord> visibleItems = dataScopeService.list(userId, type, searchQuery);
                exportExcel(response, visibleItems, type);
                return;
            }

            // Case 2: List / Search
            if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo)) {
                List<ScopeRecord> items = dataScopeService.list(userId, type, searchQuery);
                writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy danh sách thành công",
                        new ListData(items));
                return;
            }

            // Case 3: Read single record detail by ID
            Long id = parseId(pathInfo);
            if (id == null) {
                writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                        "ID bản ghi không hợp lệ", null);
                return;
            }

            DataScopeService.ReadResult result = dataScopeService.read(userId, type, id);

            switch (result.status()) {
                case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                        "Lấy dữ liệu thành công", result.record());

                case NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy bản ghi", null);

                // CRITICAL BUSINESS RULE 2: Block out-of-scope access with 403 Forbidden
                case FORBIDDEN -> writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                        "Bạn không có quyền truy cập bản ghi này do phạm vi dữ liệu.", null);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-25: Database error reading scoped entity", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi kiểm tra phạm vi dữ liệu", null);
        }
    }

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String methodOverride = request.getParameter("_method");
        if ("PUT".equalsIgnoreCase(methodOverride)) {
            doPut(request, response);
            return;
        } else if ("DELETE".equalsIgnoreCase(methodOverride)) {
            doDelete(request, response);
            return;
        }

        Long userId = currentUserId(request);
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        ScopeEntityType type = resolveEntityType(request);
        if (type == null) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài nguyên", null);
            return;
        }

        String label = extractLabelPayload(request);
        if (label == null || label.trim().isEmpty()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Tên/tiêu đề bản ghi không được để trống.", null);
            return;
        }

        try {
            ScopeRecord created = dataScopeService.create(userId, type, label);
            writeJson(response, HttpServletResponse.SC_CREATED, true,
                    "Tạo bản ghi mới thành công.", created);
        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-25: Database error creating scoped entity", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi tạo bản ghi mới", null);
        }
    }

    @Override
    protected void doPut(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long userId = currentUserId(request);
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        ScopeEntityType type = resolveEntityType(request);
        if (type == null) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài nguyên", null);
            return;
        }

        Long id = parseId(request.getPathInfo());
        if (id == null) {
            String paramId = request.getParameter("id");
            if (paramId != null) {
                try {
                    id = Long.parseLong(paramId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (id == null || id <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Thiếu ID bản ghi cần cập nhật", null);
            return;
        }

        String label = extractLabelPayload(request);
        if (label == null || label.trim().isEmpty()) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Tên/tiêu đề bản ghi không được để trống.", null);
            return;
        }

        try {
            DataScopeService.OperationResult result = dataScopeService.update(userId, type, id, label);

            switch (result.status()) {
                case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                        "Cập nhật bản ghi thành công.", result.record());

                case NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy bản ghi", null);

                // CRITICAL BUSINESS RULE 2: Block out-of-scope update with 403 Forbidden
                case FORBIDDEN -> writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                        "Bạn không có quyền chỉnh sửa bản ghi này do phạm vi dữ liệu.", null);
            }

        } catch (IllegalArgumentException e) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false, e.getMessage(), null);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-25: Database error updating scoped entity", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi cập nhật bản ghi", null);
        }
    }

    @Override
    protected void doDelete(
            HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Long userId = currentUserId(request);
        if (userId == null) {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, false,
                    "Yêu cầu đăng nhập", null);
            return;
        }

        ScopeEntityType type = resolveEntityType(request);
        if (type == null) {
            writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                    "Không tìm thấy tài nguyên", null);
            return;
        }

        Long id = parseId(request.getPathInfo());
        if (id == null) {
            String paramId = request.getParameter("id");
            if (paramId != null) {
                try {
                    id = Long.parseLong(paramId.trim());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (id == null || id <= 0) {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST, false,
                    "Thiếu ID bản ghi cần xóa", null);
            return;
        }

        try {
            DataScopeService.ReadStatus status = dataScopeService.delete(userId, type, id);

            switch (status) {
                case SUCCESS -> writeJson(response, HttpServletResponse.SC_OK, true,
                        "Xóa bản ghi thành công.", null);

                case NOT_FOUND -> writeJson(response, HttpServletResponse.SC_NOT_FOUND, false,
                        "Không tìm thấy bản ghi", null);

                // CRITICAL BUSINESS RULE 2: Block out-of-scope delete with 403 Forbidden
                case FORBIDDEN -> writeJson(response, HttpServletResponse.SC_FORBIDDEN, false,
                        "Bạn không có quyền xóa bản ghi này do phạm vi dữ liệu.", null);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "CRM-25: Database error deleting scoped entity", e);
            writeJson(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, false,
                    "Lỗi hệ thống khi xóa bản ghi", null);
        }
    }

    // === Helpers ===

    private ScopeEntityType resolveEntityType(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        ScopeEntityType type = ScopeEntityType.fromServletPath(servletPath);
        if (type != null) {
            return type;
        }
        String requestUri = request.getRequestURI();
        return ScopeEntityType.fromServletPath(requestUri);
    }

    private Long currentUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }

        Object directUserId = session.getAttribute("userId");
        if (directUserId instanceof Number number && number.longValue() > 0) {
            return number.longValue();
        }
        if (directUserId instanceof String text) {
            try {
                long parsed = Long.parseLong(text);
                if (parsed > 0) return parsed;
            } catch (NumberFormatException ignored) {}
        }

        Object currentUser = session.getAttribute(SessionKey.CURRENT_USER);
        if (currentUser instanceof User u && u.getId() > 0) {
            return u.getId();
        }
        if (currentUser instanceof Map<?, ?> map) {
            Object id = map.get("id");
            if (id instanceof Number n && n.longValue() > 0) return n.longValue();
            if (id instanceof String s) {
                try {
                    long parsed = Long.parseLong(s);
                    if (parsed > 0) return parsed;
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    private Long parseId(String pathInfo) {
        if (pathInfo == null || pathInfo.isBlank() || "/".equals(pathInfo.trim())) {
            return null;
        }
        String value = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        int slashIdx = value.indexOf('/');
        if (slashIdx != -1) {
            value = value.substring(0, slashIdx);
        }

        if (value.isBlank()) {
            return null;
        }

        try {
            long id = Long.parseLong(value);
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractSearchQuery(HttpServletRequest request) {
        String q = request.getParameter("search");
        if (q == null || q.isBlank()) {
            q = request.getParameter("q");
        }
        if (q == null || q.isBlank()) {
            q = request.getParameter("keyword");
        }
        return q;
    }

    private boolean isExportRequest(HttpServletRequest request, String pathInfo) {
        if (pathInfo != null && (pathInfo.startsWith("/export") || pathInfo.contains("/export"))) {
            return true;
        }
        String exportParam = request.getParameter("export");
        String formatParam = request.getParameter("format");
        return "true".equalsIgnoreCase(exportParam) || "1".equals(exportParam) || "csv".equalsIgnoreCase(formatParam);
    }

    private String extractLabelPayload(HttpServletRequest request) throws IOException {
        String contentType = request.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            try {
                JsonObject json = GSON.fromJson(request.getReader(), JsonObject.class);
                if (json != null) {
                    if (json.has("name") && !json.get("name").isJsonNull()) {
                        return json.get("name").getAsString();
                    }
                    if (json.has("label") && !json.get("label").isJsonNull()) {
                        return json.get("label").getAsString();
                    }
                    if (json.has("subject") && !json.get("subject").isJsonNull()) {
                        return json.get("subject").getAsString();
                    }
                    if (json.has("quoteNumber") && !json.get("quoteNumber").isJsonNull()) {
                        return json.get("quoteNumber").getAsString();
                    }
                    if (json.has("quote_number") && !json.get("quote_number").isJsonNull()) {
                        return json.get("quote_number").getAsString();
                    }
                }
            } catch (JsonSyntaxException ignored) {}
            return null;
        }

        String param = request.getParameter("name");
        if (param == null) param = request.getParameter("label");
        if (param == null) param = request.getParameter("subject");
        if (param == null) param = request.getParameter("quoteNumber");
        if (param == null) param = request.getParameter("quote_number");
        return param;
    }

    private void exportExcel(
            HttpServletResponse response,
            List<ScopeRecord> items,
            ScopeEntityType type) throws IOException {

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"crm-" + type.tableName() + "-export.xlsx\""
        );

        try (var workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Dữ liệu");
            var header = sheet.createRow(0);
            String[] columns = {"id", "label", "ownerUserId", "ownerTeamId"};
            for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);
            int index = 1;
            for (ScopeRecord item : items) {
                var row = sheet.createRow(index++);
                // Text preserves BIGINT precision and prevents formula interpretation.
                row.createCell(0).setCellValue(Long.toString(item.id()));
                row.createCell(1).setCellValue(item.label() == null ? "" : item.label());
                row.createCell(2).setCellValue(Long.toString(item.ownerUserId()));
                row.createCell(3).setCellValue(item.ownerTeamId() == null ? "" : item.ownerTeamId().toString());
            }
            workbook.write(response.getOutputStream());
        }
    }

    private void writeJson(
            HttpServletResponse response,
            int status,
            boolean success,
            String message,
            Object data) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json; charset=UTF-8");

        GSON.toJson(
                new ApiResponse(success, message, data),
                response.getWriter()
        );
    }

    private record ListData(List<ScopeRecord> items) {}
    private record ApiResponse(boolean success, String message, Object data) {}
}
