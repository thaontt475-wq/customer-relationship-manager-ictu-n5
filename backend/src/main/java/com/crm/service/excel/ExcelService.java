package com.crm.service.excel;

import com.crm.dao.excel.UserImportDAO;
import com.crm.dto.excel.ImportErrorDetail;
import com.crm.dto.excel.ImportReportResult;
import com.crm.dto.excel.ImportRowData;
import com.crm.util.DBConnection;
import com.crm.util.PasswordUtil;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Service managing Excel operations for CRM-32:
 * 1. Sample template generation (.xlsx / .csv).
 * 2. Parsing and validating rows from uploaded Excel/CSV files.
 * 3. Skipping error rows and batch-persisting valid rows to MySQL.
 * 4. Producing detailed import reports.
 */
public class ExcelService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s.]{8,20}$");
    private static final String DEFAULT_PASSWORD = "Password@123";

    // Cache of parsed batches for preview-to-confirm workflow (TTL handled via timestamp)
    private static final Map<String, CachedBatch> BATCH_CACHE = new ConcurrentHashMap<>();

    private final UserImportDAO userImportDAO;

    public ExcelService() {
        this.userImportDAO = new UserImportDAO();
    }

    public ExcelService(UserImportDAO userImportDAO) {
        this.userImportDAO = userImportDAO != null ? userImportDAO : new UserImportDAO();
    }

    /**
     * Generate sample Excel (.xlsx) template for user import.
     */
    public byte[] generateTemplate(String format) throws IOException {
        if ("csv".equalsIgnoreCase(format)) {
            return generateCsvTemplate();
        }
        return generateXlsxTemplate();
    }

    private byte[] generateXlsxTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Danh sách người dùng");

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Data Style
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            String[] headers = {
                    "Họ và tên (*)",
                    "Địa chỉ Email (*)",
                    "Tên đăng nhập",
                    "Số điện thoại",
                    "Vai trò",
                    "Nhóm kinh doanh"
            };

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample rows
            String[][] sampleData = {
                    {"Nguyễn Văn An", "nguyenvanan@crmdemo.vn", "an.nv", "0912345678", "Sales Rep", "Miền Bắc"},
                    {"Trần Thị Bình", "tranthibinh@crmdemo.vn", "binh.tt", "0987654321", "Team Lead", "Miền Bắc"},
                    {"Lê Hoàng Cường", "lehoangcuong@crmdemo.vn", "cuong.lh", "0901122334", "Accountant", ""}
            };

            for (int r = 0; r < sampleData.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < sampleData[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(sampleData[r][c]);
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 4500));
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] generateCsvTemplate() {
        String csvContent = "Họ và tên (*),Địa chỉ Email (*),Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\r\n"
                + "Nguyễn Văn An,nguyenvanan@crmdemo.vn,an.nv,0912345678,Sales Rep,Miền Bắc\r\n"
                + "Trần Thị Bình,tranthibinh@crmdemo.vn,binh.tt,0987654321,Team Lead,Miền Bắc\r\n"
                + "Lê Hoàng Cường,lehoangcuong@crmdemo.vn,cuong.lh,0901122334,Accountant,\r\n";
        return ("\uFEFF" + csvContent).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Parse and validate an uploaded file (Step 2: Preview).
     * Populates validRows, errorRows, and stores batch in BATCH_CACHE.
     */
    public ImportReportResult parseAndValidate(InputStream inputStream, String fileName)
            throws IOException, SQLException {

        List<ImportRowData> rawRows = extractRows(inputStream, fileName);
        return validateExtractedRows(rawRows);
    }

    /**
     * Direct upload and import: parses, validates, skips error rows, saves valid rows,
     * and returns the comprehensive report in a single call.
     */
    public ImportReportResult importDirect(InputStream inputStream, String fileName, long actorUserId)
            throws IOException, SQLException {

        ImportReportResult report = parseAndValidate(inputStream, fileName);
        if (report.getValidRows() != null && !report.getValidRows().isEmpty()) {
            executePersistence(report.getValidRows());
            report.setSuccessRows(report.getValidRows().size());
            report.setItems(report.getValidRows());
        } else {
            report.setSuccessRows(0);
        }
        report.setFailedRows(report.getErrorRows() != null ? report.getErrorRows().size() : 0);
        return report;
    }

    /**
     * Confirm and execute import for a previously previewed batch (Step 3: Confirm).
     * Bỏ qua dòng sai & Lưu dòng đúng vào Database.
     */
    public ImportReportResult confirmImport(String batchToken) throws SQLException {
        if (batchToken == null || batchToken.isBlank()) {
            throw new IllegalArgumentException("Mã batchToken không hợp lệ");
        }

        CachedBatch cached = BATCH_CACHE.get(batchToken);
        if (cached == null || cached.isExpired()) {
            BATCH_CACHE.remove(batchToken);
            throw new IllegalStateException("Phiên tải lên đã hết hạn hoặc không tồn tại. Vui lòng tải lại tệp.");
        }

        List<ImportRowData> validRows = cached.result.getValidRows();
        int createdCount = 0;

        if (validRows != null && !validRows.isEmpty()) {
            createdCount = executePersistence(validRows);
        }

        ImportReportResult finalReport = new ImportReportResult();
        finalReport.setBatchToken(batchToken);
        finalReport.setTotalRows(cached.result.getTotalRows());
        finalReport.setSuccessRows(createdCount);
        finalReport.setFailedRows(cached.result.getErrorRows().size());
        finalReport.setValidRows(validRows);
        finalReport.setErrorRows(cached.result.getErrorRows());
        finalReport.setErrors(cached.result.getErrors());
        finalReport.setItems(validRows);

        // Remove from cache after successful execution
        BATCH_CACHE.remove(batchToken);
        return finalReport;
    }

    // === Core validation & persistence logic ===

    private ImportReportResult validateExtractedRows(List<ImportRowData> rawRows) throws SQLException {
        ImportReportResult result = new ImportReportResult();
        String batchToken = "import_" + UUID.randomUUID().toString().replace("-", "");
        result.setBatchToken(batchToken);
        result.setTotalRows(rawRows.size());

        if (rawRows.isEmpty()) {
            result.setSuccessRows(0);
            result.setFailedRows(0);
            return result;
        }

        // Collect all non-blank emails and usernames for database lookup
        Set<String> emailsToCheck = new HashSet<>();
        Set<String> usernamesToCheck = new HashSet<>();

        for (ImportRowData row : rawRows) {
            if (row.getEmail() != null && !row.getEmail().isBlank()) {
                emailsToCheck.add(row.getEmail().trim().toLowerCase(Locale.ROOT));
            }
            if (row.getUsername() != null && !row.getUsername().isBlank()) {
                usernamesToCheck.add(row.getUsername().trim().toLowerCase(Locale.ROOT));
            }
        }

        Set<String> existingDbEmails;
        Set<String> existingDbUsernames;
        Map<String, Long> roleMap;
        Map<String, Long> teamMap;

        try (Connection conn = DBConnection.getConnection()) {
            existingDbEmails = userImportDAO.findExistingEmails(conn, emailsToCheck);
            existingDbUsernames = userImportDAO.findExistingUsernames(conn, usernamesToCheck);
            roleMap = userImportDAO.findAllRolesMap(conn);
            teamMap = userImportDAO.findAllTeamsMap(conn);
        }

        Set<String> seenEmailsInFile = new HashSet<>();
        Set<String> seenUsernamesInFile = new HashSet<>();

        List<ImportRowData> validList = new ArrayList<>();
        List<ImportRowData> errorList = new ArrayList<>();

        for (ImportRowData row : rawRows) {
            validateRow(row, seenEmailsInFile, seenUsernamesInFile, existingDbEmails, existingDbUsernames, roleMap, teamMap);

            if (row.isValid()) {
                validList.add(row);
            } else {
                errorList.add(row);
                for (String err : row.getErrors()) {
                    result.addError(new ImportErrorDetail(row.getRowNum(), "Row", err, row.getEmail()));
                }
            }
        }

        result.setValidRows(validList);
        result.setErrorRows(errorList);
        result.setSuccessRows(validList.size());
        result.setFailedRows(errorList.size());

        // Cache for 2-step confirmation (valid for 30 minutes)
        BATCH_CACHE.put(batchToken, new CachedBatch(result, System.currentTimeMillis() + 30 * 60 * 1000));
        cleanExpiredBatches();

        return result;
    }

    private void validateRow(ImportRowData row,
                             Set<String> seenEmailsInFile,
                             Set<String> seenUsernamesInFile,
                             Set<String> existingDbEmails,
                             Set<String> existingDbUsernames,
                             Map<String, Long> roleMap,
                             Map<String, Long> teamMap) {

        // 1. Full name validation
        if (row.getFullName() == null || row.getFullName().isBlank()) {
            row.addError("Họ và tên không được để trống.");
        } else if (row.getFullName().length() > 255) {
            row.addError("Họ và tên vượt quá 255 ký tự.");
        }

        // 2. Email validation
        if (row.getEmail() == null || row.getEmail().isBlank()) {
            row.addError("Địa chỉ email không được để trống.");
        } else {
            String normEmail = row.getEmail().trim().toLowerCase(Locale.ROOT);
            row.setEmail(normEmail);

            if (!EMAIL_PATTERN.matcher(normEmail).matches()) {
                row.addError("Định dạng email không hợp lệ: " + normEmail);
            } else if (seenEmailsInFile.contains(normEmail)) {
                row.addError("Email trùng lặp với dòng khác trong tệp: " + normEmail);
            } else if (existingDbEmails.contains(normEmail)) {
                row.addError("Email đã tồn tại trong hệ thống: " + normEmail);
            } else {
                seenEmailsInFile.add(normEmail);
            }
        }

        // 3. Username validation
        String rawUsername = row.getUsername();
        if (rawUsername == null || rawUsername.isBlank()) {
            // Auto-generate username from email prefix if blank
            if (row.getEmail() != null && row.getEmail().contains("@")) {
                rawUsername = row.getEmail().substring(0, row.getEmail().indexOf('@')).trim();
                row.setUsername(rawUsername);
            }
        }

        if (rawUsername != null && !rawUsername.isBlank()) {
            String normUsername = rawUsername.trim().toLowerCase(Locale.ROOT);
            row.setUsername(normUsername);

            if (normUsername.length() > 100) {
                row.addError("Tên đăng nhập không được vượt quá 100 ký tự.");
            } else if (seenUsernamesInFile.contains(normUsername)) {
                row.addError("Tên đăng nhập trùng lặp với dòng khác trong tệp: " + normUsername);
            } else if (existingDbUsernames.contains(normUsername)) {
                row.addError("Tên đăng nhập đã tồn tại trong hệ thống: " + normUsername);
            } else {
                seenUsernamesInFile.add(normUsername);
            }
        }

        // 4. Phone validation
        if (row.getPhone() != null && !row.getPhone().isBlank()) {
            String cleanPhone = row.getPhone().trim();
            row.setPhone(cleanPhone);
            if (!PHONE_PATTERN.matcher(cleanPhone).matches()) {
                row.addError("Số điện thoại không đúng định dạng.");
            }
        }

        // 5. Role validation & resolution
        String roleStr = (row.getRole() != null && !row.getRole().isBlank()) ? row.getRole().trim() : "Sales Rep";
        row.setRole(roleStr);
        Long roleId = roleMap.get(roleStr.toLowerCase(Locale.ROOT));
        if (roleId == null) {
            // Try fuzzy matching
            for (Map.Entry<String, Long> entry : roleMap.entrySet()) {
                if (entry.getKey().contains(roleStr.toLowerCase(Locale.ROOT))
                        || roleStr.toLowerCase(Locale.ROOT).contains(entry.getKey())) {
                    roleId = entry.getValue();
                    break;
                }
            }
        }

        if (roleId == null) {
            row.addError("Vai trò không tồn tại trong hệ thống: " + roleStr);
        } else {
            row.setResolvedRoleId(roleId);
        }

        // 6. Team validation & resolution
        Long teamId = null;
        if (row.getTeam() != null && !row.getTeam().isBlank()) {
            String teamStr = row.getTeam().trim();
            row.setTeam(teamStr);
            teamId = teamMap.get(teamStr.toLowerCase(Locale.ROOT));
            if (teamId == null) {
                for (Map.Entry<String, Long> entry : teamMap.entrySet()) {
                    if (entry.getKey().contains(teamStr.toLowerCase(Locale.ROOT))
                            || teamStr.toLowerCase(Locale.ROOT).contains(entry.getKey())) {
                        teamId = entry.getValue();
                        break;
                    }
                }
            }
            if (teamId == null) {
                row.addError("Nhóm kinh doanh không tồn tại trong hệ thống: " + teamStr);
            } else {
                row.setResolvedTeamId(teamId);
            }
        }

        // CRM-29 Rule 2: If role is Team Lead, team is mandatory!
        boolean isTeamLead = roleStr.toLowerCase(Locale.ROOT).contains("lead")
                || roleStr.toLowerCase(Locale.ROOT).contains("trưởng nhóm");
        if (isTeamLead && (teamId == null || teamId <= 0)) {
            row.addError("Vai trò 'Team Lead' (Trưởng nhóm) bắt buộc người dùng phải thuộc về ít nhất một nhóm kinh doanh.");
        }
    }

    private int executePersistence(List<ImportRowData> validRows) throws SQLException {
        if (validRows == null || validRows.isEmpty()) {
            return 0;
        }

        String defaultHash = PasswordUtil.hashPassword(DEFAULT_PASSWORD);

        try (Connection conn = DBConnection.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int count = userImportDAO.saveImportedUsers(conn, validRows, defaultHash);
                conn.commit();
                return count;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    e.addSuppressed(rollbackEx);
                }
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        }
    }

    // === File parsing helpers ===

    private List<ImportRowData> extractRows(InputStream inputStream, String fileName) throws IOException {
        String lowerName = fileName != null ? fileName.toLowerCase(Locale.ROOT) : "";
        if (lowerName.endsWith(".csv")) {
            return extractCsvRows(inputStream);
        }
        return extractExcelRows(inputStream);
    }

    private List<ImportRowData> extractExcelRows(InputStream inputStream) throws IOException {
        List<ImportRowData> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }

            int firstRowNum = sheet.getFirstRowNum();
            int lastRowNum = sheet.getLastRowNum();

            if (firstRowNum < 0 || lastRowNum < firstRowNum) {
                return rows;
            }

            // Assume first row is header, data starts at firstRowNum + 1
            for (int r = firstRowNum + 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                String fullName = getCellValueAsString(row.getCell(0));
                String email = getCellValueAsString(row.getCell(1));
                String username = getCellValueAsString(row.getCell(2));
                String phone = getCellValueAsString(row.getCell(3));
                String role = getCellValueAsString(row.getCell(4));
                String team = getCellValueAsString(row.getCell(5));

                ImportRowData data = new ImportRowData(r + 1, fullName, email, username, phone, role, team);
                rows.add(data);
            }
        }
        return rows;
    }

    private List<ImportRowData> extractCsvRows(InputStream inputStream) throws IOException {
        List<ImportRowData> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                // Skip UTF-8 BOM if present
                if (lineNum == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (lineNum == 1 || line.trim().isEmpty()) {
                    continue; // skip header or empty lines
                }

                String[] parts = line.split(",", -1);
                String fullName = parts.length > 0 ? cleanCsvField(parts[0]) : "";
                String email = parts.length > 1 ? cleanCsvField(parts[1]) : "";
                String username = parts.length > 2 ? cleanCsvField(parts[2]) : "";
                String phone = parts.length > 3 ? cleanCsvField(parts[3]) : "";
                String role = parts.length > 4 ? cleanCsvField(parts[4]) : "";
                String team = parts.length > 5 ? cleanCsvField(parts[5]) : "";

                ImportRowData data = new ImportRowData(lineNum, fullName, email, username, phone, role, team);
                rows.add(data);
            }
        }
        return rows;
    }

    private String cleanCsvField(String field) {
        if (field == null) return "";
        String trimmed = field.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"").trim();
        }
        return trimmed;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellValueAsString(cell);
                if (val != null && !val.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == Math.floor(num)) {
                    yield String.valueOf((long) num);
                }
                yield String.valueOf(num);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception e) {
                    try {
                        double fNum = cell.getNumericCellValue();
                        if (fNum == Math.floor(fNum)) {
                            yield String.valueOf((long) fNum);
                        }
                        yield String.valueOf(fNum);
                    } catch (Exception ex) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private void cleanExpiredBatches() {
        long now = System.currentTimeMillis();
        BATCH_CACHE.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private record CachedBatch(ImportReportResult result, long expireAt) {
        boolean isExpired() {
            return isExpired(System.currentTimeMillis());
        }

        boolean isExpired(long now) {
            return now > expireAt;
        }
    }
}
