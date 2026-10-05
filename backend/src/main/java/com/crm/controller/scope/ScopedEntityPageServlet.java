package com.crm.controller.scope;

import com.crm.controller.ServerForms;
import com.crm.service.scope.*;
import jakarta.servlet.*;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.*;

/** Server-rendered entry points for the four scoped modules. */
@WebServlet({"/customers", "/opportunities", "/activities", "/quotes"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB
        maxFileSize = 10 * 1024 * 1024,        // 10 MB
        maxRequestSize = 20 * 1024 * 1024      // 20 MB
)
public class ScopedEntityPageServlet extends HttpServlet {
    private final DataScopeService service;
    public ScopedEntityPageServlet() { this(new DataScopeService()); }
    public ScopedEntityPageServlet(DataScopeService service) { this.service = service; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        var type = ScopeEntityType.fromServletPath("/api" + req.getServletPath());
        res.setHeader("Cache-Control", "no-store");

        // Handle template download: ?download=template&format=xlsx or csv
        if ("template".equals(req.getParameter("download"))) {
            handleDownloadTemplate(type, req.getParameter("format"), res);
            return;
        }

        // Handle export: ?export=1 or ?action=export
        if ("1".equals(req.getParameter("export")) || "export".equals(req.getParameter("action"))) {
            try {
                var records = service.list(ServerForms.actor(req), type, req.getParameter("q"));
                ScopedEntityServlet.exportExcel(req, res, records, type);
                return;
            } catch (SQLException e) {
                getServletContext().log("Cannot export scoped records", e);
                res.sendError(500);
                return;
            }
        }

        req.setAttribute("moduleTitle", switch (type) {
            case CUSTOMERS -> "Khách hàng";
            case OPPORTUNITIES -> "Cơ hội";
            case ACTIVITIES -> "Hoạt động";
            case QUOTES -> "Báo giá";
        });
        try {
            if (req.getParameter("id") != null) {
                var result = service.read(ServerForms.actor(req), type, ServerForms.positive(req.getParameter("id")));
                if (result.status() != DataScopeService.ReadStatus.SUCCESS) {
                    res.sendError(result.status() == DataScopeService.ReadStatus.FORBIDDEN ? 403 : 404);
                    return;
                }
                req.setAttribute("record", result.record());
            } else {
                var records = service.list(ServerForms.actor(req), type, req.getParameter("q"));
                int pages = Math.max(1, (records.size() + 19) / 20);
                int page = 1;
                try { page = Math.max(1, Math.min(pages, Integer.parseInt(req.getParameter("page")))); }
                catch (NumberFormatException ignored) { }
                int from = Math.min(records.size(), (page - 1) * 20);
                req.setAttribute("records", records.subList(from, Math.min(records.size(), from + 20)));
                req.setAttribute("pageNumber", page);
                req.setAttribute("pageCount", pages);
            }
            req.getRequestDispatcher("/jsp/shared/scoped-records.jsp").forward(req, res);
        } catch (IllegalArgumentException e) { res.sendError(400); }
        catch (SQLException e) {
            getServletContext().log("Cannot load scoped records", e);
            res.sendError(500);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        if (!ServerForms.authorize(req, res, false)) return;
        if (!ServerForms.checkCsrf(req, res)) return;
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        var type = ScopeEntityType.fromServletPath("/api" + req.getServletPath());

        String action = req.getParameter("action");
        Part filePart = null;
        try {
            filePart = req.getPart("file");
        } catch (Exception ignored) {}

        // Check if import action or file upload or direct demo sample
        if ("import".equals(action) || (filePart != null && filePart.getSize() > 0) || "1".equals(req.getParameter("directSample"))) {
            handleImport(req, res, type, filePart);
            return;
        }

        // Single record manual create
        String name = req.getParameter("name");
        if (name == null || name.isBlank()) {
            name = req.getParameter("fullName");
        }
        if (name == null || name.isBlank()) {
            name = req.getParameter("label");
        }
        if (name == null || name.isBlank()) {
            name = req.getParameter("subject");
        }

        if (name != null && !name.isBlank()) {
            try {
                ScopeRecord created = service.create(ServerForms.actor(req), type, name.trim());
                ServerForms.setToast(req, "success", "Tạo mới thành công", "Đã thêm \"" + name.trim() + "\" vào danh sách.");

                HttpSession session = req.getSession(false);
                if (session != null && created != null) {
                    Map<String, String> meta = new HashMap<>();
                    meta.put("id", String.valueOf(created.id()));
                    meta.put("name", name.trim());
                    meta.put("taxId", req.getParameter("taxId"));
                    meta.put("industry", req.getParameter("industry"));
                    meta.put("tagBadge", req.getParameter("tagBadge"));
                    meta.put("phone", req.getParameter("phone"));
                    meta.put("email", req.getParameter("email"));
                    meta.put("address", req.getParameter("address"));
                    meta.put("revenue", req.getParameter("revenue"));
                    meta.put("status", req.getParameter("status"));

                    saveCustomRecordToSession(session, type, meta);
                }
            } catch (SQLException | IllegalArgumentException e) {
                getServletContext().log("Cannot create scoped record", e);
                ServerForms.setToast(req, "error", "Lỗi tạo mới", e.getMessage());
            }
        }
        res.sendRedirect(req.getContextPath() + req.getServletPath() + "?created=1");
    }

    private void handleImport(HttpServletRequest req, HttpServletResponse res, ScopeEntityType type, Part filePart) throws IOException {
        HttpSession session = req.getSession(false);
        Long actor = ServerForms.actor(req);
        int importedCount = 0;

        // Check if direct sample was chosen
        if ("1".equals(req.getParameter("directSample"))) {
            List<Map<String, String>> demoSamples = getDemoCustomerSamples();
            for (Map<String, String> sample : demoSamples) {
                try {
                    ScopeRecord created = service.create(actor, type, sample.get("name"));
                    if (session != null && created != null) {
                        sample.put("id", String.valueOf(created.id()));
                        saveCustomRecordToSession(session, type, sample);
                    }
                    importedCount++;
                } catch (Exception e) {
                    getServletContext().log("Error importing demo sample customer: " + sample.get("name"), e);
                }
            }
            ServerForms.setToast(req, "success", "Nhập Excel thành công", "Đã nhập thành công " + importedCount + " khách hàng mẫu vào hệ thống.");
            res.sendRedirect(req.getContextPath() + req.getServletPath() + "?imported=" + importedCount);
            return;
        }

        // Process uploaded file
        if (filePart != null && filePart.getSize() > 0) {
            String submittedFileName = filePart.getSubmittedFileName();
            String lowerName = (submittedFileName != null) ? submittedFileName.toLowerCase(Locale.ROOT) : "";

            if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) {
                try (Workbook workbook = WorkbookFactory.create(filePart.getInputStream())) {
                    Sheet sheet = workbook.getSheetAt(0);
                    for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                        Row row = sheet.getRow(r);
                        if (row == null) continue;
                        String name = getCellValue(row.getCell(0));
                        if (name == null || name.isBlank()) continue;

                        String taxId = getCellValue(row.getCell(1));
                        String industry = getCellValue(row.getCell(2));
                        String tagBadge = getCellValue(row.getCell(3));
                        String status = getCellValue(row.getCell(4));
                        String phone = getCellValue(row.getCell(5));
                        String email = getCellValue(row.getCell(6));
                        String address = getCellValue(row.getCell(7));
                        String revenue = getCellValue(row.getCell(8));

                        try {
                            ScopeRecord created = service.create(actor, type, name.trim());
                            if (session != null && created != null) {
                                Map<String, String> meta = new HashMap<>();
                                meta.put("id", String.valueOf(created.id()));
                                meta.put("name", name.trim());
                                meta.put("taxId", taxId);
                                meta.put("industry", industry);
                                meta.put("tagBadge", tagBadge);
                                meta.put("status", status);
                                meta.put("phone", phone);
                                meta.put("email", email);
                                meta.put("address", address);
                                meta.put("revenue", revenue);
                                saveCustomRecordToSession(session, type, meta);
                            }
                            importedCount++;
                        } catch (Exception e) {
                            getServletContext().log("Error importing excel row " + r, e);
                        }
                    }
                } catch (Exception e) {
                    getServletContext().log("Cannot parse excel file", e);
                    ServerForms.setToast(req, "error", "Lỗi đọc tệp Excel", "Không thể đọc nội dung tệp Excel: " + e.getMessage());
                    res.sendRedirect(req.getContextPath() + req.getServletPath() + "?import_error=1");
                    return;
                }
            } else {
                // Parse CSV file
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(filePart.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    boolean isHeader = true;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        if (isHeader) {
                            isHeader = false;
                            continue;
                        }
                        String[] cols = line.split("[,;\\t]");
                        if (cols.length == 0 || cols[0].trim().isBlank()) continue;

                        String name = cols[0].trim().replace("\"", "");
                        String taxId = cols.length > 1 ? cols[1].trim().replace("\"", "") : "";
                        String industry = cols.length > 2 ? cols[2].trim().replace("\"", "") : "";
                        String tagBadge = cols.length > 3 ? cols[3].trim().replace("\"", "") : "";
                        String status = cols.length > 4 ? cols[4].trim().replace("\"", "") : "";
                        String phone = cols.length > 5 ? cols[5].trim().replace("\"", "") : "";
                        String email = cols.length > 6 ? cols[6].trim().replace("\"", "") : "";
                        String address = cols.length > 7 ? cols[7].trim().replace("\"", "") : "";
                        String revenue = cols.length > 8 ? cols[8].trim().replace("\"", "") : "";

                        try {
                            ScopeRecord created = service.create(actor, type, name);
                            if (session != null && created != null) {
                                Map<String, String> meta = new HashMap<>();
                                meta.put("id", String.valueOf(created.id()));
                                meta.put("name", name);
                                meta.put("taxId", taxId);
                                meta.put("industry", industry);
                                meta.put("tagBadge", tagBadge);
                                meta.put("status", status);
                                meta.put("phone", phone);
                                meta.put("email", email);
                                meta.put("address", address);
                                meta.put("revenue", revenue);
                                saveCustomRecordToSession(session, type, meta);
                            }
                            importedCount++;
                        } catch (Exception e) {
                            getServletContext().log("Error importing csv row: " + name, e);
                        }
                    }
                } catch (Exception e) {
                    getServletContext().log("Cannot parse csv file", e);
                    ServerForms.setToast(req, "error", "Lỗi đọc tệp CSV", e.getMessage());
                    res.sendRedirect(req.getContextPath() + req.getServletPath() + "?import_error=1");
                    return;
                }
            }

            ServerForms.setToast(req, "success", "Nhập dữ liệu thành công", "Đã nhập thành công " + importedCount + " khách hàng vào hệ thống.");
            res.sendRedirect(req.getContextPath() + req.getServletPath() + "?imported=" + importedCount);
            return;
        }

        ServerForms.setToast(req, "warning", "Chưa chọn tệp", "Vui lòng chọn tệp Excel hoặc CSV để nhập dữ liệu.");
        res.sendRedirect(req.getContextPath() + req.getServletPath() + "?import=1");
    }

    private void handleDownloadTemplate(ScopeEntityType type, String format, HttpServletResponse res) throws IOException {
        if ("csv".equalsIgnoreCase(format)) {
            res.setContentType("text/csv; charset=UTF-8");
            res.setHeader("Content-Disposition", "attachment; filename=\"CRM_Mau_Nhap_Khach_Hang.csv\"");
            try (var out = res.getOutputStream()) {
                out.write(0xEF); out.write(0xBB); out.write(0xBF); // UTF-8 BOM
                String csv = "Tên Doanh nghiệp / Khách hàng,Mã số thuế,Lĩnh vực,Phân loại,Trạng thái,Số điện thoại,Email,Địa chỉ,Doanh thu dự kiến\n"
                        + "Công ty Cổ phần Công nghệ Nam Á,0109876543,Công nghệ phần mềm,Khách hàng VIP,Đang hoạt động,024 3800 8888,contact@nama.com.vn,Tòa nhà Discovery Complex - Cầu Giấy - Hà Nội,1.500.000.000 đ\n"
                        + "Tập đoàn Bất động sản Đại Dương,0312456789,Dịch vụ doanh nghiệp,Khách hàng chiến lược,Đang hoạt động,028 3911 2233,info@daiduonggroup.vn,180 Nguyễn Thị Minh Khai - Quận 3 - TP.HCM,4.200.000.000 đ\n"
                        + "Công ty TNHH Dược phẩm Đông Nam,0105678123,Y tế & Dược phẩm,Tiêu chuẩn,Đang hoạt động,024 3766 5544,duocdongnam@gmail.com,Lô CN-05 KCN Nội Bài - Sóc Sơn - Hà Nội,850.000.000 đ\n";
                out.write(csv.getBytes(StandardCharsets.UTF_8));
            }
            return;
        }

        // Default XLSX template
        res.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        res.setHeader("Content-Disposition", "attachment; filename=\"CRM_Mau_Nhap_Khach_Hang.xlsx\"");
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("KhachHang");
            String[] headers = {
                    "Tên Doanh nghiệp / Khách hàng", "Mã số thuế", "Lĩnh vực", "Phân loại",
                    "Trạng thái", "Số điện thoại", "Email", "Địa chỉ", "Doanh thu dự kiến"
            };

            CellStyle headerStyle = wb.createCellStyle();
            Font font = wb.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row hRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = hRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] samples = {
                    {"Công ty Cổ phần Công nghệ Nam Á", "0109876543", "Công nghệ phần mềm", "Khách hàng VIP", "Đang hoạt động", "024 3800 8888", "contact@nama.com.vn", "Tòa nhà Discovery Complex, Cầu Giấy, Hà Nội", "1.500.000.000 đ"},
                    {"Tập đoàn Bất động sản Đại Dương", "0312456789", "Dịch vụ doanh nghiệp", "Khách hàng chiến lược", "Đang hoạt động", "028 3911 2233", "info@daiduonggroup.vn", "180 Nguyễn Thị Minh Khai, Quận 3, TP.HCM", "4.200.000.000 đ"},
                    {"Công ty TNHH Dược phẩm Đông Nam", "0105678123", "Y tế & Dược phẩm", "Tiêu chuẩn", "Đang hoạt động", "024 3766 5544", "duocdongnam@gmail.com", "Lô CN-05 KCN Nội Bài, Sóc Sơn, Hà Nội", "850.000.000 đ"}
            };

            for (int r = 0; r < samples.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < samples[r].length; c++) {
                    row.createCell(c).setCellValue(samples[r][c]);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 22 * 256);
            }

            wb.write(res.getOutputStream());
        }
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try { yield cell.getStringCellValue(); }
                catch (Exception e) { yield String.valueOf(cell.getNumericCellValue()); }
            }
            default -> "";
        };
    }

    @SuppressWarnings("unchecked")
    private void saveCustomRecordToSession(HttpSession session, ScopeEntityType type, Map<String, String> meta) {
        List<Map<String, String>> customList = (List<Map<String, String>>) session.getAttribute("custom_created_records_" + type.name());
        if (customList == null) {
            customList = new ArrayList<>();
            session.setAttribute("custom_created_records_" + type.name(), customList);
        }
        customList.add(0, meta);
    }

    private List<Map<String, String>> getDemoCustomerSamples() {
        List<Map<String, String>> list = new ArrayList<>();
        list.add(createSampleMeta("Công ty Cổ phần Giải pháp Số Nam Á", "0109876543", "Công nghệ phần mềm", "Khách hàng VIP", "active", "024 3800 8888", "contact@nama.vn", "Discovery Complex, Cầu Giấy, Hà Nội", "1.500.000.000 đ"));
        list.add(createSampleMeta("Tập đoàn Bất động sản Đại Dương", "0312456789", "Dịch vụ doanh nghiệp", "Khách hàng chiến lược", "active", "028 3911 2233", "info@daiduonggroup.vn", "180 Nguyễn Thị Minh Khai, Quận 3, TP.HCM", "4.200.000.000 đ"));
        list.add(createSampleMeta("Công ty TNHH Dược phẩm Đông Nam", "0105678123", "Y tế & Dược phẩm", "Tiêu chuẩn", "active", "024 3766 5544", "duocdongnam@gmail.com", "Lô CN-05 KCN Nội Bài, Sóc Sơn, Hà Nội", "850.000.000 đ"));
        list.add(createSampleMeta("Tổng Công ty Năng lượng Xanh Việt Nam", "0107890123", "Sản xuất / Chế tạo", "Tập đoàn mẹ", "active", "024 3999 7788", "admin@greenenergy.vn", "Tòa Keangnam, Mễ Trì, Từ Liêm, Hà Nội", "6.800.000.000 đ"));
        list.add(createSampleMeta("Công ty Cổ phần Bán lẻ Thăng Long", "0106543210", "Bán lẻ / Phân phối", "Đối tác cấp 1", "active", "024 3555 1234", "retail@thanglong.vn", "45 Hàng Bài, Hoàn Kiếm, Hà Nội", "2.100.000.000 đ"));
        return list;
    }

    private Map<String, String> createSampleMeta(String name, String taxId, String industry, String tag, String status, String phone, String email, String address, String revenue) {
        Map<String, String> m = new HashMap<>();
        m.put("name", name);
        m.put("taxId", taxId);
        m.put("industry", industry);
        m.put("tagBadge", tag);
        m.put("status", status);
        m.put("phone", phone);
        m.put("email", email);
        m.put("address", address);
        m.put("revenue", revenue);
        return m;
    }
}
