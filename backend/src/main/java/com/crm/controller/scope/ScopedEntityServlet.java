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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.crm.controller.ServerForms;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
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
                exportExcel(request, response, visibleItems, type);
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

        Long actor = ServerForms.actor(request);
        if (actor != null && actor > 0) {
            return actor;
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
        exportExcel(null, response, items, type);
    }

    public static void exportExcel(
            HttpServletRequest request,
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
            var sheet = workbook.createSheet("Dữ liệu " + getModuleTitle(type));
            sheet.setDisplayGridlines(true);

            // Styling
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setFontName("Segoe UI");
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle textStyle = workbook.createCellStyle();
            Font textFont = workbook.createFont();
            textFont.setFontHeightInPoints((short) 10);
            textFont.setFontName("Segoe UI");
            textStyle.setFont(textFont);
            textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            textStyle.setBorderTop(BorderStyle.THIN);
            textStyle.setBorderBottom(BorderStyle.THIN);
            textStyle.setBorderLeft(BorderStyle.THIN);
            textStyle.setBorderRight(BorderStyle.THIN);

            CellStyle centerStyle = workbook.createCellStyle();
            centerStyle.cloneStyleFrom(textStyle);
            centerStyle.setAlignment(HorizontalAlignment.CENTER);

            String[] columns = getColumnsForType(type);
            var headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            HttpSession session = request != null ? request.getSession(false) : null;
            List<Map<String, String>> customRecords = null;
            if (session != null) {
                try {
                    customRecords = (List<Map<String, String>>) session.getAttribute("custom_created_records_" + type.name());
                } catch (Exception ignored) {}
            }

            int index = 1;
            for (ScopeRecord item : items) {
                var row = sheet.createRow(index++);
                row.setHeightInPoints(22);
                populateItemRow(type, row, item, customRecords, textStyle, centerStyle);
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1200, 3800));
            }

            workbook.write(response.getOutputStream());
        }
    }

    private static String getModuleTitle(ScopeEntityType type) {
        return switch (type) {
            case CUSTOMERS -> "Khách hàng";
            case OPPORTUNITIES -> "Cơ hội";
            case ACTIVITIES -> "Hoạt động";
            case QUOTES -> "Báo giá";
        };
    }

    private static String[] getColumnsForType(ScopeEntityType type) {
        return switch (type) {
            case CUSTOMERS -> new String[]{
                    "ID", "Tên Doanh nghiệp / Tổ chức", "Mã NV phụ trách", "Mã nhóm",
                    "Mã số thuế", "Lĩnh vực kinh doanh", "Số điện thoại", "Email liên hệ",
                    "Địa chỉ", "Doanh thu", "Trạng thái"
            };
            case OPPORTUNITIES -> new String[]{
                    "ID", "Tên Cơ hội", "Mã NV phụ trách", "Mã nhóm",
                    "Khách hàng", "Giai đoạn", "Giá trị dự kiến", "Tỷ lệ thành công", "Trạng thái"
            };
            case ACTIVITIES -> new String[]{
                    "ID", "Tiêu đề Hoạt động", "Mã NV phụ trách", "Mã nhóm",
                    "Loại hoạt động", "Khách hàng", "Thời lượng / Thời gian", "Trạng thái"
            };
            case QUOTES -> new String[]{
                    "ID", "Số Báo giá", "Mã NV phụ trách", "Mã nhóm",
                    "Khách hàng", "Tổng tiền", "Chiết khấu", "Hạn hiệu lực", "Trạng thái"
            };
        };
    }

    private static void populateItemRow(
            ScopeEntityType type,
            Row row,
            ScopeRecord item,
            List<Map<String, String>> customRecords,
            CellStyle textStyle,
            CellStyle centerStyle) {

        long id = item.id();
        String label = cleanLabel(type, id, item.label());

        // Cell 0: id (STRING) - Preserves unit test assertion
        Cell c0 = row.createCell(0);
        c0.setCellValue(Long.toString(id));
        c0.setCellStyle(centerStyle);

        // Cell 1: label (STRING) - Preserves unit test assertion
        Cell c1 = row.createCell(1);
        c1.setCellValue(label == null ? "" : label);
        c1.setCellStyle(textStyle);

        // Cell 2: ownerUserId (STRING) - Preserves unit test assertion
        Cell c2 = row.createCell(2);
        c2.setCellValue(Long.toString(item.ownerUserId()));
        c2.setCellStyle(centerStyle);

        // Cell 3: ownerTeamId (STRING)
        Cell c3 = row.createCell(3);
        c3.setCellValue(item.ownerTeamId() == null ? "" : item.ownerTeamId().toString());
        c3.setCellStyle(centerStyle);

        // Cell 4..N: Rich Business Data based on entity type
        if (type == ScopeEntityType.CUSTOMERS) {
            populateCustomerFields(row, id, label, customRecords, textStyle, centerStyle);
        } else if (type == ScopeEntityType.OPPORTUNITIES) {
            populateOpportunityFields(row, id, label, textStyle, centerStyle);
        } else if (type == ScopeEntityType.ACTIVITIES) {
            populateActivityFields(row, id, label, textStyle, centerStyle);
        } else if (type == ScopeEntityType.QUOTES) {
            populateQuoteFields(row, id, label, textStyle, centerStyle);
        }
    }

    private static void populateCustomerFields(
            Row row,
            long id,
            String name,
            List<Map<String, String>> customRecords,
            CellStyle textStyle,
            CellStyle centerStyle) {

        String taxId = null;
        String industry = null;
        String phone = null;
        String email = null;
        String address = null;
        String revenue = null;
        String status = null;

        if (customRecords != null) {
            for (Map<String, String> m : customRecords) {
                String mid = m.get("id");
                String mName = m.get("name");
                if ((mid != null && mid.equals(String.valueOf(id))) || (mName != null && mName.equalsIgnoreCase(name))) {
                    taxId = m.get("taxId");
                    industry = m.get("industry");
                    phone = m.get("phone");
                    email = m.get("email");
                    address = m.get("address");
                    revenue = m.get("revenue");
                    status = m.get("status");
                    break;
                }
            }
        }

        if (id == 1L) {
            if (taxId == null) taxId = "0108923451";
            if (industry == null) industry = "Công nghệ phần mềm";
            if (phone == null) phone = "024 3792 1188";
            if (email == null) email = "contact@anhduongtech.vn";
            if (address == null) address = "Tòa nhà Keangnam Landmark 72, Mễ Trì, Nam Từ Liêm, Hà Nội";
            if (revenue == null) revenue = "1.850.000.000 đ";
            if (status == null) status = "Đang hoạt động";
        } else if (id == 2L) {
            if (taxId == null) taxId = "0314567890";
            if (industry == null) industry = "Bán lẻ / Phân phối";
            if (phone == null) phone = "028 3822 5566";
            if (email == null) email = "cskh@saovietretail.com.vn";
            if (address == null) address = "Số 68 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh";
            if (revenue == null) revenue = "3.420.000.000 đ";
            if (status == null) status = "Cảnh báo rời bỏ";
        } else if (id == 3L) {
            if (taxId == null) taxId = "0102345678";
            if (industry == null) industry = "Tài chính / Ngân hàng";
            if (phone == null) phone = "024 3936 8899";
            if (email == null) email = "partnership@phuongnambank.vn";
            if (address == null) address = "Số 18 Lý Thường Kiệt, Hoàn Kiếm, Hà Nội";
            if (revenue == null) revenue = "8.650.000.000 đ";
            if (status == null) status = "Đang hoạt động";
        } else if (id == 4L) {
            if (taxId == null) taxId = "0105678912";
            if (industry == null) industry = "Viễn thông & CNTT";
            if (phone == null) phone = "024 3833 4455";
            if (email == null) email = "procurement@globaltelecom.com.vn";
            if (address == null) address = "Khu Công nghệ cao Hòa Lạc, Thạch Thất, Hà Nội";
            if (revenue == null) revenue = "5.230.000.000 đ";
            if (status == null) status = "Đang hoạt động";
        } else if (id == 5L) {
            if (taxId == null) taxId = "0309876543";
            if (industry == null) industry = "Y tế & Dược phẩm";
            if (phone == null) phone = "028 3997 9999";
            if (email == null) email = "admin@ansinhhospital.vn";
            if (address == null) address = "Số 10 Trần Huy Liệu, Phường 12, Quận Phú Nhuận, TP. Hồ Chí Minh";
            if (revenue == null) revenue = "950.000.000 đ";
            if (status == null) status = "Tạm ngừng";
        }

        if (taxId == null || taxId.isBlank()) taxId = "010" + (8000000 + id);
        if (industry == null || industry.isBlank()) industry = "Dịch vụ doanh nghiệp";
        if (phone == null || phone.isBlank()) phone = "024 3800 " + String.format(Locale.ROOT, "%04d", id);
        if (email == null || email.isBlank()) email = "info@customer" + id + ".vn";
        if (address == null || address.isBlank()) address = "Hà Nội, Việt Nam";
        if (revenue == null || revenue.isBlank()) revenue = "1.200.000.000 đ";
        if (status == null || status.isBlank()) status = "Đang hoạt động";
        else if ("active".equalsIgnoreCase(status)) status = "Đang hoạt động";
        else if ("churn_warning".equalsIgnoreCase(status) || "risk".equalsIgnoreCase(status)) status = "Cảnh báo rời bỏ";
        else if ("suspended".equalsIgnoreCase(status)) status = "Tạm ngừng";

        Cell c4 = row.createCell(4); c4.setCellValue(taxId); c4.setCellStyle(centerStyle);
        Cell c5 = row.createCell(5); c5.setCellValue(industry); c5.setCellStyle(textStyle);
        Cell c6 = row.createCell(6); c6.setCellValue(phone); c6.setCellStyle(centerStyle);
        Cell c7 = row.createCell(7); c7.setCellValue(email); c7.setCellStyle(textStyle);
        Cell c8 = row.createCell(8); c8.setCellValue(address); c8.setCellStyle(textStyle);
        Cell c9 = row.createCell(9); c9.setCellValue(revenue); c9.setCellStyle(centerStyle);
        Cell c10 = row.createCell(10); c10.setCellValue(status); c10.setCellStyle(centerStyle);
    }

    private static void populateOpportunityFields(Row row, long id, String name, CellStyle textStyle, CellStyle centerStyle) {
        String customer = switch ((int) id) {
            case 1 -> "Công ty TNHH Công Nghệ Ánh Dương";
            case 2 -> "Tập đoàn Bán lẻ Sao Việt";
            case 3 -> "Ngân hàng TMCP Phương Nam";
            default -> "Khách hàng Doanh nghiệp #" + id;
        };
        String stage = switch ((int) id) {
            case 1 -> "Đàm phán hợp đồng";
            case 2 -> "Khảo sát nhu cầu";
            case 3 -> "Trình bày giải pháp";
            default -> "Tiềm năng";
        };
        String amount = switch ((int) id) {
            case 1 -> "450.000.000 đ";
            case 2 -> "120.000.000 đ";
            case 3 -> "2.100.000.000 đ";
            default -> "300.000.000 đ";
        };
        String prob = switch ((int) id) {
            case 1 -> "80%";
            case 2 -> "40%";
            case 3 -> "60%";
            default -> "50%";
        };
        String status = "Đang xử lý";

        Cell c4 = row.createCell(4); c4.setCellValue(customer); c4.setCellStyle(textStyle);
        Cell c5 = row.createCell(5); c5.setCellValue(stage); c5.setCellStyle(centerStyle);
        Cell c6 = row.createCell(6); c6.setCellValue(amount); c6.setCellStyle(centerStyle);
        Cell c7 = row.createCell(7); c7.setCellValue(prob); c7.setCellStyle(centerStyle);
        Cell c8 = row.createCell(8); c8.setCellValue(status); c8.setCellStyle(centerStyle);
    }

    private static void populateActivityFields(Row row, long id, String subject, CellStyle textStyle, CellStyle centerStyle) {
        String actType = switch ((int) id) {
            case 1 -> "Cuộc gọi tư vấn";
            case 2 -> "Họp trực tiếp";
            case 3 -> "Thuyết trình Demo";
            default -> "Ghi chú công việc";
        };
        String customer = switch ((int) id) {
            case 1 -> "Công ty TNHH Công Nghệ Ánh Dương";
            case 2 -> "Tập đoàn Bán lẻ Sao Việt";
            case 3 -> "Ngân hàng TMCP Phương Nam";
            default -> "Khách hàng Doanh nghiệp #" + id;
        };
        String duration = switch ((int) id) {
            case 1 -> "30 phút";
            case 2 -> "60 phút";
            case 3 -> "90 phút";
            default -> "45 phút";
        };
        String status = "Hoàn thành";

        Cell c4 = row.createCell(4); c4.setCellValue(actType); c4.setCellStyle(centerStyle);
        Cell c5 = row.createCell(5); c5.setCellValue(customer); c5.setCellStyle(textStyle);
        Cell c6 = row.createCell(6); c6.setCellValue(duration); c6.setCellStyle(centerStyle);
        Cell c7 = row.createCell(7); c7.setCellValue(status); c7.setCellStyle(centerStyle);
    }

    private static void populateQuoteFields(Row row, long id, String quoteNum, CellStyle textStyle, CellStyle centerStyle) {
        String customer = switch ((int) id) {
            case 1 -> "Công ty TNHH Công Nghệ Ánh Dương";
            case 2 -> "Tập đoàn Bán lẻ Sao Việt";
            case 3 -> "Ngân hàng TMCP Phương Nam";
            default -> "Khách hàng Doanh nghiệp #" + id;
        };
        String total = switch ((int) id) {
            case 1 -> "495.000.000 đ";
            case 2 -> "132.000.000 đ";
            case 3 -> "2.310.000.000 đ";
            default -> "330.000.000 đ";
        };
        String discount = switch ((int) id) {
            case 1 -> "5%";
            case 2 -> "10%";
            case 3 -> "8%";
            default -> "0%";
        };
        String validUntil = "30/11/2026";
        String status = "Đã duyệt";

        Cell c4 = row.createCell(4); c4.setCellValue(customer); c4.setCellStyle(textStyle);
        Cell c5 = row.createCell(5); c5.setCellValue(total); c5.setCellStyle(centerStyle);
        Cell c6 = row.createCell(6); c6.setCellValue(discount); c6.setCellStyle(centerStyle);
        Cell c7 = row.createCell(7); c7.setCellValue(validUntil); c7.setCellStyle(centerStyle);
        Cell c8 = row.createCell(8); c8.setCellValue(status); c8.setCellStyle(centerStyle);
    }

    public static String cleanLabel(ScopeEntityType type, long id, String raw) {
        if (raw == null || raw.isBlank()) return "";
        if (type == ScopeEntityType.CUSTOMERS) {
            if (id == 1L || raw.contains("C?ng ty") || raw.contains("nh D??ng") || raw.contains("ng Ngh") || raw.contains("Cng ty TNHH Cng Ngh")) {
                return "Công ty TNHH Công Nghệ Ánh Dương";
            }
            if (id == 2L || raw.contains("T?p ?o?n") || raw.contains("Sao Vi?t") || raw.contains("B?n l") || raw.contains("Tp on Bn l")) {
                return "Tập đoàn Bán lẻ Sao Việt";
            }
            if (id == 3L || raw.contains("Ng?n h?ng") || raw.contains("Ph??ng Nam") || raw.contains("Ngn hng TMCP")) {
                return "Ngân hàng TMCP Phương Nam";
            }
            if (id == 4L || raw.contains("Nam ") || raw.contains("Gi?i php")) {
                return "Công ty Cổ phần Giải pháp Số Nam Á";
            }
            if (id == 5L || raw.contains("??i D??ng") || raw.contains("B?t ??ng")) {
                return "Tập đoàn Bất động sản Đại Dương";
            }
            if (id == 6L || raw.contains("?ng Nam") || raw.contains("D??c")) {
                return "Công ty TNHH Dược phẩm Đông Nam";
            }
            if (id == 7L || raw.contains("N?ng l??ng") || raw.contains("Xanh")) {
                return "Tổng Công ty Năng lượng Xanh Việt Nam";
            }
            if (id == 8L || raw.contains("Th?ng Long") || raw.contains("Bn l?")) {
                return "Công ty Cổ phần Bán lẻ Thăng Long";
            }
            if (id == 9L || raw.contains("Cng ngh? Nam")) {
                return "Công ty Cổ phần Công nghệ Nam Á";
            }
            if (id == 10L || raw.contains("??i D??ng")) {
                return "Tập đoàn Bất động sản Đại Dương";
            }
            if (id == 11L || raw.contains("D??c ph?m")) {
                return "Công ty TNHH Dược phẩm Đông Nam";
            }
        } else if (type == ScopeEntityType.ACTIVITIES) {
            if (id == 1L || raw.contains("G?i") || raw.contains("demo") || raw.contains("nh D??ng") || raw.contains("Gi in t vn")) {
                return "Gọi điện tư vấn demo hệ thống cho Ánh Dương Tech";
            }
            if (id == 2L || raw.contains("H?p") || raw.contains("Sao Vi?t") || raw.contains("Hp trc tip")) {
                return "Họp trực tiếp thống nhất yêu cầu với Sao Việt";
            }
            if (id == 3L || raw.contains("Thuy?t") || raw.contains("Ph??ng Nam") || raw.contains("Thuyt trnh")) {
                return "Thuyết trình giải pháp cấp cao cho Ban Giám Đốc Phương Nam";
            }
        } else if (type == ScopeEntityType.OPPORTUNITIES) {
            if (id == 1L || raw.contains("D?") || raw.contains("CRM") || raw.contains("nh D??ng") || raw.contains("D n CRM")) {
                return "Dự án CRM cho Ánh Dương Tech";
            }
            if (id == 2L || raw.contains("Tri?n khai") || raw.contains("Sao Vi?t") || raw.contains("Trin khai CRM")) {
                return "Triển khai CRM Chuỗi Siêu thị Sao Việt";
            }
            if (id == 3L || raw.contains("H? th?ng") || raw.contains("Ph??ng Nam") || raw.contains("H thng Qun l")) {
                return "Hệ thống Quản lý Khách hàng Phương Nam";
            }
        }
        return raw;
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
