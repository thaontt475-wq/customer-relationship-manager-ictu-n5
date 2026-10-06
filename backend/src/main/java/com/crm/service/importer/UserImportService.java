package com.crm.service.importer;

import com.crm.dao.users.UserManagementDAO;
import com.crm.util.PasswordPolicy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserImportService {
    private static final List<String> COLUMNS = List.of("fullName", "email", "password", "status");
    private static final Duration BATCH_TTL = Duration.ofMinutes(15);
    private static final Map<String, Batch> BATCHES = new ConcurrentHashMap<>();
    private final UserManagementDAO dao = new UserManagementDAO();

    public void writeTemplate(OutputStream output) throws Exception {
        writeTemplate(output, "valid");
    }

    public void writeTemplate(OutputStream output, String type) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            for (int i = 0; i < COLUMNS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(COLUMNS.get(i));
                cell.setCellStyle(headerStyle);
            }

            if ("invalid".equalsIgnoreCase(type) || "error".equalsIgnoreCase(type)) {
                // Template with intentional validation errors for testing
                Object[][] testRows = {
                    {"Nguyễn Văn Hợp Lệ", "user.hople@company.com", "User@123456", "ACTIVE"},
                    {"", "thieuten@company.com", "User@123456", "ACTIVE"}, // Lỗi: Thiếu fullName
                    {"Trần Văn Sai Email", "email_khong_hop_le", "User@123456", "ACTIVE"}, // Lỗi: Email sai định dạng
                    {"Lê Trùng Lặp 1", "duplicate.email@company.com", "User@123456", "ACTIVE"},
                    {"Lê Trùng Lặp 2", "duplicate.email@company.com", "User@123456", "ACTIVE"}, // Lỗi: Email bị trùng trong file
                    {"Tài Khoản Đã Tồn Tại", "admin@company.com", "Admin@123456", "ACTIVE"}, // Lỗi: Email đã tồn tại trong DB
                    {"Hoàng Mật Khẩu Yếu", "matkhauyeu@company.com", "123456", "ACTIVE"}, // Lỗi: Mật khẩu không đạt chính sách bảo mật
                    {"Vũ Trạng Thái Sai", "trangthaisai@company.com", "User@123456", "PENDING"} // Lỗi: Status không hợp lệ
                };

                for (int r = 0; r < testRows.length; r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < COLUMNS.size(); c++) {
                        row.createCell(c).setCellValue(String.valueOf(testRows[r][c]));
                    }
                }
            } else {
                // Template with 100% valid records
                Object[][] validRows = {
                    {"Nguyễn Văn An", "nguyenvanan.sales@company.com", "User@123456", "ACTIVE"},
                    {"Trần Thị Bích", "tranthibich.sales@company.com", "User@123456", "ACTIVE"},
                    {"Lê Hoàng Cường", "lehoangcuong.tech@company.com", "User@123456", "ACTIVE"},
                    {"Phạm Thu Dung", "phamthudung.crm@company.com", "User@123456", "ACTIVE"},
                    {"Vũ Minh Đức", "vuminhduc.leads@company.com", "User@123456", "INACTIVE"}
                };

                for (int r = 0; r < validRows.length; r++) {
                    Row row = sheet.createRow(r + 1);
                    for (int c = 0; c < COLUMNS.size(); c++) {
                        row.createCell(c).setCellValue(String.valueOf(validRows[r][c]));
                    }
                }
            }

            for (int i = 0; i < COLUMNS.size(); i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i), 6000));
            }

            workbook.write(output);
        }
    }

    public Map<String, Object> preview(InputStream input, long ownerUserId) throws Exception {
        if (ownerUserId <= 0) throw new SecurityException();
        List<RowData> rows = readRows(input);
        List<Map<String, Object>> validRows = new ArrayList<>();
        List<Map<String, Object>> errorRows = new ArrayList<>();
        List<RowData> batchRows = new ArrayList<>();
        Set<String> seenEmails = new HashSet<>();
        for (RowData row : rows) {
            boolean duplicate = !row.email().isBlank() && !seenEmails.add(row.email());
            String error = validate(row, duplicate);
            if (error == null && dao.emailExists(row.email(), null)) error = "Email đã tồn tại";
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("row", row.row());
            item.put("fullName", row.fullName());
            item.put("email", row.email());
            item.put("status", row.status());
            if (error != null) {
                item.put("error", error);
                errorRows.add(item);
            } else {
                validRows.add(item);
                batchRows.add(row);
            }
        }
        Instant now = Instant.now();
        BATCHES.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        String token = UUID.randomUUID().toString();
        BATCHES.put(token, new Batch(ownerUserId, now.plus(BATCH_TTL), List.copyOf(batchRows)));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("batchToken", token);
        result.put("validRows", validRows);
        result.put("errorRows", errorRows);
        return result;
    }

    public Map<String, Object> confirm(String batchToken, long ownerUserId) throws Exception {
        if (batchToken == null || batchToken.isBlank()) throw invalidBatch();
        Batch batch = BATCHES.get(batchToken);
        if (batch == null) throw invalidBatch();
        // An unauthorized user must not consume another user's preview.
        if (batch.ownerUserId() != ownerUserId) throw new SecurityException();
        if (!batch.expiresAt().isAfter(Instant.now())) {
            BATCHES.remove(batchToken, batch);
            throw invalidBatch();
        }
        if (!BATCHES.remove(batchToken, batch)) throw invalidBatch();
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();
        for (RowData row : batch.rows()) {
            try {
                if (dao.emailExists(row.email(), null)) {
                    skipped++;
                    continue;
                }
                dao.create(row.fullName(), row.email(), row.password(), row.status());
                created++;
            } catch (Exception e) {
                // Driver messages can include SQL or submitted values. Return a safe row error only.
                errors.add(row.email() + ": Không thể tạo người dùng");
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("errors", errors);
        return result;
    }

    private List<RowData> readRows(InputStream input) throws Exception {
        XSSFWorkbook workbook;
        try {
            workbook = new XSSFWorkbook(input);
        } catch (Exception e) {
            throw new InvalidWorkbookException("File .xlsx không hợp lệ hoặc bị hỏng");
        }
        try (workbook) {
            if (workbook.getNumberOfSheets() == 0) throw new InvalidWorkbookException("File không có worksheet");
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            Map<String, Integer> columns = readColumns(sheet.getRow(0), formatter);
            List<RowData> rows = new ArrayList<>();
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                String fullName = cell(row, columns.get("fullname"), formatter).trim();
                String email = cell(row, columns.get("email"), formatter).trim().toLowerCase(Locale.ROOT);
                String password = cell(row, columns.get("password"), formatter);
                String status = cell(row, columns.get("status"), formatter).trim().toUpperCase(Locale.ROOT);
                if (fullName.isBlank() && email.isBlank() && password.isBlank() && status.isBlank()) continue;
                if (status.isBlank()) status = "ACTIVE";
                rows.add(new RowData(i + 1, fullName, email, password, status));
            }
            return rows;
        } catch (InvalidWorkbookException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidWorkbookException("File .xlsx không hợp lệ hoặc bị hỏng");
        }
    }

    private Map<String, Integer> readColumns(Row header, DataFormatter formatter) {
        if (header == null) throw new InvalidWorkbookException("Thiếu dòng tiêu đề theo file mẫu");
        Map<String, Integer> columns = new HashMap<>();
        Set<String> required = new HashSet<>();
        for (String column : COLUMNS) required.add(column.toLowerCase(Locale.ROOT));
        for (int i = 0; i < header.getLastCellNum(); i++) {
            String name = formatter.formatCellValue(header.getCell(i)).trim().toLowerCase(Locale.ROOT);
            if (!required.contains(name)) continue;
            if (columns.putIfAbsent(name, i) != null) {
                throw new InvalidWorkbookException("Cột bắt buộc bị trùng trong dòng tiêu đề");
            }
        }
        if (!columns.keySet().containsAll(required)) {
            throw new InvalidWorkbookException("Thiếu cột bắt buộc: fullName, email, password, status");
        }
        return columns;
    }

    private String cell(Row row, int index, DataFormatter formatter) {
        return formatter.formatCellValue(row.getCell(index));
    }

    private String validate(RowData row, boolean duplicate) {
        if (row.fullName().isBlank()) return "Thiếu fullName";
        if (row.fullName().length() > 150) return "fullName không được vượt quá 150 ký tự";
        if (row.email().isBlank()) return "Thiếu email";
        if (row.email().length() > 255 || !row.email().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) return "Email không hợp lệ";
        if (duplicate) return "Email bị trùng trong file";
        if (!PasswordPolicy.isValid(row.password())
                || row.password().getBytes(StandardCharsets.UTF_8).length > 72) return "Password không hợp lệ";
        if (!"ACTIVE".equals(row.status()) && !"INACTIVE".equals(row.status())) return "Status không hợp lệ";
        return null;
    }

    private IllegalArgumentException invalidBatch() {
        return new IllegalArgumentException("Batch token không hợp lệ, đã hết hạn hoặc đã sử dụng");
    }

    private record RowData(int row, String fullName, String email, String password, String status) {}
    private record Batch(long ownerUserId, Instant expiresAt, List<RowData> rows) {}
    private static class InvalidWorkbookException extends IllegalArgumentException {
        InvalidWorkbookException(String message) { super(message); }
    }
}
