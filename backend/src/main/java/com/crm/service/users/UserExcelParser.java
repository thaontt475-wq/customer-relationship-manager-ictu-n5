package com.crm.service.users;

import com.crm.dto.users.UserImportRow;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class UserExcelParser {

    public List<UserImportRow> parse(InputStream in, String filename) throws IOException {
        if (filename != null && (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls"))) {
            return parseXlsx(in);
        } else {
            return parseCsv(in);
        }
    }

    private List<UserImportRow> parseXlsx(InputStream in) throws IOException {
        List<UserImportRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(in)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }

            Iterator<Row> rowIterator = sheet.iterator();
            if (!rowIterator.hasNext()) {
                return rows;
            }

            // Đọc dòng Header
            Row headerRow = rowIterator.next();
            Map<String, Integer> colMap = buildColumnMap(headerRow);

            int rowNum = 1;
            while (rowIterator.hasNext()) {
                rowNum++;
                Row r = rowIterator.next();
                if (isRowEmpty(r)) {
                    continue;
                }

                UserImportRow rowData = new UserImportRow(rowNum);
                rowData.setUsername(getCellValue(r, colMap.get("username")));
                rowData.setFullName(getCellValue(r, colMap.get("fullname")));
                rowData.setEmail(getCellValue(r, colMap.get("email")));
                rowData.setPhone(getCellValue(r, colMap.get("phone")));
                rowData.setRoleName(getCellValue(r, colMap.get("role")));
                rowData.setTeamName(getCellValue(r, colMap.get("team")));
                rowData.setDataScope(getCellValue(r, colMap.get("datascope")));
                rowData.setPassword(getCellValue(r, colMap.get("password")));

                rows.add(rowData);
            }
        }
        return rows;
    }

    private List<UserImportRow> parseCsv(InputStream in) throws IOException {
        List<UserImportRow> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        String line = reader.readLine();
        if (line == null) {
            return rows;
        }

        // Bỏ ký tự BOM nếu có
        if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }

        char delimiter = line.contains(";") && !line.contains(",") ? ';' : ',';
        List<String> headers = parseCsvLine(line, delimiter);
        Map<String, Integer> colMap = buildColumnMapFromList(headers);

        int rowNum = 1;
        while ((line = reader.readLine()) != null) {
            rowNum++;
            if (line.trim().isEmpty()) {
                continue;
            }

            List<String> values = parseCsvLine(line, delimiter);
            if (isListEmpty(values)) {
                continue;
            }

            UserImportRow rowData = new UserImportRow(rowNum);
            rowData.setUsername(getCsvValue(values, colMap.get("username")));
            rowData.setFullName(getCsvValue(values, colMap.get("fullname")));
            rowData.setEmail(getCsvValue(values, colMap.get("email")));
            rowData.setPhone(getCsvValue(values, colMap.get("phone")));
            rowData.setRoleName(getCsvValue(values, colMap.get("role")));
            rowData.setTeamName(getCsvValue(values, colMap.get("team")));
            rowData.setDataScope(getCsvValue(values, colMap.get("datascope")));
            rowData.setPassword(getCsvValue(values, colMap.get("password")));

            rows.add(rowData);
        }

        return rows;
    }

    private Map<String, Integer> buildColumnMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell c = headerRow.getCell(i);
            if (c == null) continue;
            String text = c.getStringCellValue().trim().toLowerCase(Locale.ROOT);
            mapColumn(map, text, i);
        }
        fillDefaultIndices(map);
        return map;
    }

    private Map<String, Integer> buildColumnMapFromList(List<String> headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            String text = headers.get(i).trim().toLowerCase(Locale.ROOT);
            mapColumn(map, text, i);
        }
        fillDefaultIndices(map);
        return map;
    }

    private void mapColumn(Map<String, Integer> map, String text, int index) {
        if (text.contains("username") || text.contains("đăng nhập") || text.contains("tài khoản")) {
            map.put("username", index);
        } else if (text.contains("full") || text.contains("họ và tên") || text.contains("họ tên") || text.contains("tên")) {
            map.put("fullname", index);
        } else if (text.contains("mail")) {
            map.put("email", index);
        } else if (text.contains("phone") || text.contains("thoại") || text.contains("sđt")) {
            map.put("phone", index);
        } else if (text.contains("role") || text.contains("vai trò") || text.contains("quyền")) {
            map.put("role", index);
        } else if (text.contains("team") || text.contains("nhóm") || text.contains("phòng ban")) {
            map.put("team", index);
        } else if (text.contains("scope") || text.contains("phạm vi")) {
            map.put("datascope", index);
        } else if (text.contains("pass") || text.contains("mật khẩu")) {
            map.put("password", index);
        }
    }

    private void fillDefaultIndices(Map<String, Integer> map) {
        // Nếu không khớp tên tiêu đề, dùng vị trí mặc định
        map.putIfAbsent("username", 0);
        map.putIfAbsent("fullname", 1);
        map.putIfAbsent("email", 2);
        map.putIfAbsent("phone", 3);
        map.putIfAbsent("role", 4);
        map.putIfAbsent("team", 5);
        map.putIfAbsent("datascope", 6);
        map.putIfAbsent("password", 7);
    }

    private String getCellValue(Row row, Integer colIndex) {
        if (colIndex == null || colIndex < 0 || colIndex >= row.getLastCellNum()) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == (long) val) {
                    yield String.valueOf((long) val);
                } else {
                    yield String.valueOf(val);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private String getCsvValue(List<String> values, Integer colIndex) {
        if (colIndex == null || colIndex < 0 || colIndex >= values.size()) {
            return null;
        }
        String val = values.get(colIndex);
        return val != null ? val.trim() : null;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = cell.toString().trim();
                if (!val.isEmpty()) return false;
            }
        }
        return true;
    }

    private boolean isListEmpty(List<String> values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<String> parseCsvLine(String line, char delimiter) {
        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '\"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '\"') {
                    cur.append('\"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (ch == delimiter && !inQuotes) {
                result.add(cur.toString().trim());
                cur.setLength(0);
            } else {
                cur.append(ch);
            }
        }
        result.add(cur.toString().trim());
        return result;
    }

    public void writeTemplateXlsx(OutputStream out) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Danh sách người dùng");

            // Tạo style cho Header
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Row header = sheet.createRow(0);
            String[] headers = {
                    "Tên đăng nhập", "Họ và tên", "Email", "Số điện thoại",
                    "Vai trò", "Nhóm kinh doanh", "Phạm vi dữ liệu", "Mật khẩu"
            };

            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dòng dữ liệu mẫu 1
            Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("nguyen.vana");
            r1.createCell(1).setCellValue("Nguyễn Văn A");
            r1.createCell(2).setCellValue("nguyenvana@crm.vn");
            r1.createCell(3).setCellValue("0912345678");
            r1.createCell(4).setCellValue("Sales Rep");
            r1.createCell(5).setCellValue("Đội Kinh Doanh 1");
            r1.createCell(6).setCellValue("SELF");
            r1.createCell(7).setCellValue("P@ssword123");

            // Dòng dữ liệu mẫu 2
            Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("tran.thib");
            r2.createCell(1).setCellValue("Trần Thị B");
            r2.createCell(2).setCellValue("tranthib@crm.vn");
            r2.createCell(3).setCellValue("0987654321");
            r2.createCell(4).setCellValue("Team Lead");
            r2.createCell(5).setCellValue("Đội Kinh Doanh 1");
            r2.createCell(6).setCellValue("TEAM");
            r2.createCell(7).setCellValue("P@ssword123");

            // Dòng dữ liệu mẫu 3
            Row r3 = sheet.createRow(3);
            r3.createCell(0).setCellValue("le.vanc");
            r3.createCell(1).setCellValue("Lê Văn C");
            r3.createCell(2).setCellValue("levanc@crm.vn");
            r3.createCell(3).setCellValue("0901234567");
            r3.createCell(4).setCellValue("Director");
            r3.createCell(5).setCellValue("");
            r3.createCell(6).setCellValue("ALL");
            r3.createCell(7).setCellValue("P@ssword123");

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
        }
    }

    public void writeTemplateCsv(Writer writer) throws IOException {
        // Ghi UTF-8 BOM
        writer.write('\uFEFF');
        writer.write("Tên đăng nhập,Họ và tên,Email,Số điện thoại,Vai trò,Nhóm kinh doanh,Phạm vi dữ liệu,Mật khẩu\r\n");
        writer.write("nguyen.vana,Nguyễn Văn A,nguyenvana@crm.vn,0912345678,Sales Rep,Đội Kinh Doanh 1,SELF,P@ssword123\r\n");
        writer.write("tran.thib,Trần Thị B,tranthib@crm.vn,0987654321,Team Lead,Đội Kinh Doanh 1,TEAM,P@ssword123\r\n");
        writer.write("le.vanc,Lê Văn C,levanc@crm.vn,0901234567,Director,,ALL,P@ssword123\r\n");
        writer.flush();
    }
}
