package com.crm.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * Bộ tạo tệp mẫu Excel chuẩn hóa cho NovaCRM (Chuẩn Enterprise XLSX):
 * 1. Mẫu nhập dữ liệu (Import Templates): Khách hàng, Lead, Người dùng
 * 2. Mẫu xuất dữ liệu chuẩn (Export Templates): Báo giá, Hợp đồng, Khách hàng, Người liên hệ
 */
public final class ExcelTemplateGenerator {

    private ExcelTemplateGenerator() {}

    // Màu sắc thương hiệu
    private static final byte[] COLOR_NAVY_DARK = new byte[]{(byte) 30, (byte) 58, (byte) 138};      // #1E3A8A (Navy Dark)
    private static final byte[] COLOR_NAVY_PRIMARY = new byte[]{(byte) 37, (byte) 99, (byte) 235};   // #2563EB (Primary Blue)
    private static final byte[] COLOR_HEADER_BG = new byte[]{(byte) 15, (byte) 23, (byte) 42};       // #0F172A (Slate 900)
    private static final byte[] COLOR_ZEBRA_LIGHT = new byte[]{(byte) 248, (byte) 250, (byte) 252};  // #F8FAFC
    private static final byte[] COLOR_BORDER_GRAY = new byte[]{(byte) 203, (byte) 213, (byte) 225};  // #CBD5E1
    private static final byte[] COLOR_TOTAL_BG = new byte[]{(byte) 238, (byte) 242, (byte) 255};     // #EEF2FF
    private static final byte[] COLOR_SUCCESS_BG = new byte[]{(byte) 220, (byte) 252, (byte) 231};   // #DCFCE7
    private static final byte[] COLOR_SUCCESS_TEXT = new byte[]{(byte) 22, (byte) 101, (byte) 52};   // #166534

    /* =========================================================================
       1. MẪU NHẬP KHÁCH HÀNG (CUSTOMER IMPORT TEMPLATE)
       ========================================================================= */
    public static void buildCustomerImportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Customers");
            sheet.setDisplayGridlines(true);

            CellStyle headerStyle = createHeaderStyle(wb, COLOR_NAVY_DARK);
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);

            // Columns definition
            List<String> headers = List.of(
                "name", "taxCode", "status", "email", "phone", "website", "address", "industryId", "companySizeId"
            );

            // Row 0: Column codes for parser
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(28);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
            }

            // Dữ liệu mẫu thực tế
            Object[][] sampleRows = {
                {"Công ty Cổ phần Công nghệ NovaTech", "0108923456", "CHINH_THUC", "contact@novatech.vn", "02438889999", "https://novatech.vn", "Tầng 8, Tòa Keangnam, Mễ Trì, Hà Nội", "1", "3"},
                {"Tập đoàn Bán lẻ Sao Việt", "0312456789", "DANG_GIAO_DICH", "info@saovietretail.com", "02839998888", "https://saoviet.com.vn", "123 Nguyễn Thị Minh Khai, Quận 1, TP.HCM", "2", "4"},
                {"Công ty TNHH Giải pháp Số Nam Á", "0401889922", "TIEM_NANG", "admin@nama-solutions.vn", "0988776655", "https://nama-solutions.vn", "45 Lê Duẩn, Quận Hải Châu, Đà Nẵng", "1", "2"},
                {"Tổng Công ty Cơ Khí Chế Tạo Miền Bắc", "0200112233", "NGUNG_HOP_TAC", "vanphong@cokhimienbac.vn", "02253778899", "https://cokhimienbac.vn", "Lô 5 KCN Đình Vũ, Hải Phòng", "3", "5"}
            };

            for (int r = 0; r < sampleRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);
                for (int col = 0; col < sampleRows[r].length; col++) {
                    Cell c = row.createCell(col);
                    c.setCellValue(sampleRows[r][col].toString());
                    if (col == 1 || col == 2 || col == 4 || col == 7 || col == 8) {
                        c.setCellStyle(isZebra ? zebraCenterStyle : centerStyle);
                    } else {
                        c.setCellStyle(isZebra ? zebraTextStyle : textStyle);
                    }
                }
            }

            // Set column widths
            int[] widths = {10000, 4500, 4800, 7500, 5000, 7000, 11000, 3500, 3800};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 1);
            wb.write(out);
        }
    }

    /* =========================================================================
       2. MẪU NHẬP KHÁCH HÀNG TIỀM NĂNG (LEAD IMPORT TEMPLATE)
       ========================================================================= */
    public static void buildLeadImportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Leads");
            sheet.setDisplayGridlines(true);

            CellStyle headerStyle = createHeaderStyle(wb, COLOR_NAVY_PRIMARY);
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);

            List<String> headers = List.of("fullName", "companyName", "email", "phone", "sourceCode", "notes");

            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(28);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
            }

            Object[][] sampleRows = {
                {"Nguyễn Văn An", "Công ty TNHH Thương mại Minh Phúc", "an.nguyen@minhphuc.vn", "0901234567", "WEBSITE", "Quan tâm phần mềm CRM gói 50 người dùng"},
                {"Trần Thị Bích", "CP Bất động sản Địa Ốc Xanh", "bich.tran@diaocthanh.com", "0918765432", "EVENT", "Khách mời hội thảo TechExpo 2026, cần gọi lại sau 15h"},
                {"Lê Hoàng Cường", "Chuỗi Khách sạn & Nghỉ dưỡng Nam Á", "cuong.le@namahotel.vn", "0933445566", "REFERRAL", "Được anh Nguyễn Văn Thắng giới thiệu sang"},
                {"Phạm Minh Đức", "Công ty Cổ phần Vận tải Ánh Dương", "duc.pm@anhduonglogistics.vn", "0988112233", "COLD_CALL", "Cần demo tính năng theo dõi hợp đồng và công nợ"}
            };

            for (int r = 0; r < sampleRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);
                for (int col = 0; col < sampleRows[r].length; col++) {
                    Cell c = row.createCell(col);
                    c.setCellValue(sampleRows[r][col].toString());
                    if (col == 3 || col == 4) {
                        c.setCellStyle(isZebra ? zebraCenterStyle : centerStyle);
                    } else {
                        c.setCellStyle(isZebra ? zebraTextStyle : textStyle);
                    }
                }
            }

            int[] widths = {6500, 9500, 7500, 4800, 4200, 12000};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 1);
            wb.write(out);
        }
    }

    /* =========================================================================
       3. MẪU NHẬP NGƯỜI DÙNG (USER IMPORT TEMPLATE)
       ========================================================================= */
    public static void buildUserImportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Users");
            sheet.setDisplayGridlines(true);

            CellStyle headerStyle = createHeaderStyle(wb, new byte[]{(byte) 49, (byte) 46, (byte) 129}); // Indigo
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);

            List<String> headers = List.of("fullName", "email", "password", "status");

            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(28);
            for (int i = 0; i < headers.size(); i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(headers.get(i));
                c.setCellStyle(headerStyle);
            }

            Object[][] sampleRows = {
                {"Hoàng Trọng Thái", "thai.ht@crmdemo.vn", "User@123456", "ACTIVE"},
                {"Nguyễn Văn Thắng", "thang.nv@crmdemo.vn", "User@123456", "ACTIVE"},
                {"Trần Thu Hà", "ha.tt@crmdemo.vn", "User@123456", "ACTIVE"},
                {"Lê Hoàng Cường", "cuong.lh@crmdemo.vn", "User@123456", "ACTIVE"},
                {"Đỗ Minh Tuấn", "tuan.dm@crmdemo.vn", "User@123456", "ACTIVE"}
            };

            for (int r = 0; r < sampleRows.length; r++) {
                Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);
                for (int col = 0; col < sampleRows[r].length; col++) {
                    Cell c = row.createCell(col);
                    c.setCellValue(sampleRows[r][col].toString());
                    if (col == 3) {
                        c.setCellStyle(isZebra ? zebraCenterStyle : centerStyle);
                    } else {
                        c.setCellStyle(isZebra ? zebraTextStyle : textStyle);
                    }
                }
            }

            int[] widths = {7000, 8500, 5500, 4500};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 1);
            wb.write(out);
        }
    }

    /* =========================================================================
       4. MẪU XUẤT BÁO GIÁ KINH DOANH (QUOTATIONS EXPORT REPORT)
       ========================================================================= */
    public static void buildQuotationsExportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Báo Giá");
            sheet.setDisplayGridlines(true);

            // Banner Title Styles
            CellStyle titleStyle = createBannerTitleStyle(wb);
            CellStyle metaStyle = createMetaStyle(wb);
            CellStyle colHeaderStyle = createHeaderStyle(wb, COLOR_HEADER_BG);
            CellStyle moneyStyle = createMoneyStyle(wb, false);
            CellStyle zebraMoneyStyle = createMoneyStyle(wb, true);
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);
            CellStyle totalRowStyle = createTotalRowStyle(wb, HorizontalAlignment.RIGHT);
            CellStyle totalLabelStyle = createTotalRowStyle(wb, HorizontalAlignment.CENTER);

            // Row 0: Title Banner
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell cTitle = r0.createCell(0);
            cTitle.setCellValue("NOVACRM ENTERPRISE - BẢNG KÊ DANH SÁCH BÁO GIÁ THƯƠNG MẠI");
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            // Row 1: Subtitle / Meta
            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(20);
            Cell cMeta = r1.createCell(0);
            cMeta.setCellValue("Thời gian kết xuất: Tháng 10/2026 | Người kết xuất: Quản trị viên hệ thống | Đơn vị tiền tệ: VNĐ");
            cMeta.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            sheet.createRow(2).setHeightInPoints(8); // Spacer

            // Row 3: Headers
            String[] headers = {
                "STT", "Mã Báo Giá", "Tiêu Đề Báo Giá", "Khách Hàng", "Giá Chưa Thuế (VNĐ)",
                "VAT (10%)", "Tổng Thanh Toán (VNĐ)", "Hạn Hiệu Lực", "Trạng Thái", "Người Phụ Trách"
            };
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            // Data rows
            Object[][] data = {
                {1, "BG-67210", "Báo giá Phần mềm Quản trị Quan hệ Khách hàng NovaCRM Enterprise", "Công ty Cổ phần Công nghệ Mới", 17186364.0, 1718636.0, 18905000.0, "30/11/2026", "ĐÃ DUYỆT (APPROVED)", "Nông Quang Tiệp"},
                {2, "BG-2026-002", "Gói Dịch vụ Triển khai Hệ thống CRM & Hạ tầng Cloud 12 Tháng", "Tập đoàn Bán lẻ Sao Việt", 45000000.0, 4500000.0, 49500000.0, "15/11/2026", "ĐANG ĐÀM PHÁN (SENT)", "Hoàng Trọng Thái"},
                {3, "BG-2026-003", "Bản quyền Nâng cấp Module Phân tích & Tích hợp ERP Doanh nghiệp", "Công ty TNHH Công Nghệ Ánh Dương", 120000000.0, 12000000.0, 132000000.0, "20/12/2026", "ĐÃ DUYỆT (APPROVED)", "Nguyễn Văn Thắng"},
                {4, "BG-2026-004", "Gói Đào tạo Chuyên sâu & Chuyển giao Vận hành NovaCRM cho 100 User", "Công ty CP Giải pháp Số Nam Á", 28000000.0, 2800000.0, 30800000.0, "05/11/2026", "BẢN NHÁP (DRAFT)", "Trần Thu Hà"},
                {5, "BG-2026-005", "Dịch vụ Tùy biến Quy trình Lead Scoring & Phân bổ Tự động", "Tổng Công ty Cơ Khí Miền Bắc", 35000000.0, 3500000.0, 38500000.0, "10/11/2026", "CHỜ DUYỆT (PENDING)", "Lê Hoàng Cường"}
            };

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 4);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);

                row.createCell(0).setCellValue((Integer) data[r][0]);
                row.getCell(0).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(1).setCellValue(data[r][1].toString());
                row.getCell(1).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(2).setCellValue(data[r][2].toString());
                row.getCell(2).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(3).setCellValue(data[r][3].toString());
                row.getCell(3).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                Cell cPre = row.createCell(4);
                cPre.setCellValue((Double) data[r][4]);
                cPre.setCellStyle(isZebra ? zebraMoneyStyle : moneyStyle);

                Cell cVat = row.createCell(5);
                cVat.setCellValue((Double) data[r][5]);
                cVat.setCellStyle(isZebra ? zebraMoneyStyle : moneyStyle);

                Cell cTotal = row.createCell(6);
                cTotal.setCellValue((Double) data[r][6]);
                cTotal.setCellStyle(isZebra ? zebraMoneyStyle : moneyStyle);

                row.createCell(7).setCellValue(data[r][7].toString());
                row.getCell(7).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(8).setCellValue(data[r][8].toString());
                row.getCell(8).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(9).setCellValue(data[r][9].toString());
                row.getCell(9).setCellStyle(isZebra ? zebraTextStyle : textStyle);
            }

            // Summary row
            int sumRowIdx = data.length + 4;
            Row sumRow = sheet.createRow(sumRowIdx);
            sumRow.setHeightInPoints(26);
            for (int col = 0; col < 10; col++) {
                Cell sc = sumRow.createCell(col);
                sc.setCellStyle(totalRowStyle);
            }
            sumRow.getCell(0).setCellValue("TỔNG CỘNG DOANH THU BÁO GIÁ:");
            sumRow.getCell(0).setCellStyle(totalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(sumRowIdx, sumRowIdx, 0, 3));

            Cell sumPre = sumRow.getCell(4);
            sumPre.setCellFormula(String.format("SUM(E5:E%d)", sumRowIdx));
            sumPre.setCellStyle(totalRowStyle);

            Cell sumVat = sumRow.getCell(5);
            sumVat.setCellFormula(String.format("SUM(F5:F%d)", sumRowIdx));
            sumVat.setCellStyle(totalRowStyle);

            Cell sumTotal = sumRow.getCell(6);
            sumTotal.setCellFormula(String.format("SUM(G5:G%d)", sumRowIdx));
            sumTotal.setCellStyle(totalRowStyle);

            int[] widths = {1800, 3800, 13000, 9500, 5800, 4200, 6200, 3600, 5800, 4800};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 4);
            wb.write(out);
        }
    }

    /* =========================================================================
       5. MẪU XUẤT HỢP ĐỒNG KINH TẾ (CONTRACTS EXPORT REPORT)
       ========================================================================= */
    public static void buildContractsExportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Hợp Đồng");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createBannerTitleStyle(wb);
            CellStyle metaStyle = createMetaStyle(wb);
            CellStyle colHeaderStyle = createHeaderStyle(wb, COLOR_NAVY_DARK);
            CellStyle moneyStyle = createMoneyStyle(wb, false);
            CellStyle zebraMoneyStyle = createMoneyStyle(wb, true);
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);
            CellStyle totalRowStyle = createTotalRowStyle(wb, HorizontalAlignment.RIGHT);
            CellStyle totalLabelStyle = createTotalRowStyle(wb, HorizontalAlignment.CENTER);

            // Banner Title
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell cTitle = r0.createCell(0);
            cTitle.setCellValue("NOVACRM ENTERPRISE - BÁO CÁO TỔNG HỢP HỢP ĐỒNG KINH TẾ");
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 9));

            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(20);
            Cell cMeta = r1.createCell(0);
            cMeta.setCellValue("Trạng thái: Đang theo dõi hợp đồng | Thời gian kết xuất: 2026 | Đơn vị tiền tệ: VNĐ");
            cMeta.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 9));

            sheet.createRow(2).setHeightInPoints(8);

            String[] headers = {
                "STT", "Số Hợp Đồng", "Tên Hợp Đồng", "Khách Hàng", "Báo Giá Gốc",
                "Giá Trị Hợp Đồng (VNĐ)", "Ngày Bắt Đầu", "Ngày Kết Thúc", "Trạng Thái", "Điều Khoản Thanh Toán"
            };
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            Object[][] data = {
                {1, "HD-2026-001", "Hợp đồng Triển khai Hệ thống CRM Enterprise Toàn diện", "Công ty TNHH Công Nghệ Ánh Dương", "BG-67100", 250000000.0, "01/01/2026", "31/12/2026", "HIỆU LỰC (ACTIVE)", "50% tạm ứng khi ký, 50% sau nghiệm thu"},
                {2, "HD-2026-002", "Hợp đồng Dịch vụ Tư vấn & Đào tạo CRM Chuyên sâu", "Tập đoàn Bán lẻ Sao Việt", "BG-67180", 45000000.0, "15/02/2026", "15/08/2026", "HIỆU LỰC (ACTIVE)", "Net 30 - Thanh toán trong 30 ngày kể từ hóa đơn"},
                {3, "HD-2026-003", "Hợp đồng Nâng cấp Hạ tầng & Bảo trì Cloud 1 Năm", "Công ty Cổ phần Giải pháp Số Nam Á", "BG-67205", 68000000.0, "01/03/2026", "01/03/2027", "BẢN NHÁP (DRAFT)", "Thanh toán từng quý (4 đợt)"},
                {4, "HD-2026-004", "Hợp đồng Cung cấp Bản quyền Phần mềm NovaCRM", "Công ty Cổ phần Công nghệ Mới", "BG-67210", 18905000.0, "17/10/2026", "17/10/2027", "HIỆU LỰC (ACTIVE)", "100% thanh toán trước khi bàn giao bản quyền"}
            };

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 4);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);

                row.createCell(0).setCellValue((Integer) data[r][0]);
                row.getCell(0).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(1).setCellValue(data[r][1].toString());
                row.getCell(1).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(2).setCellValue(data[r][2].toString());
                row.getCell(2).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(3).setCellValue(data[r][3].toString());
                row.getCell(3).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(4).setCellValue(data[r][4].toString());
                row.getCell(4).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                Cell cVal = row.createCell(5);
                cVal.setCellValue((Double) data[r][5]);
                cVal.setCellStyle(isZebra ? zebraMoneyStyle : moneyStyle);

                row.createCell(6).setCellValue(data[r][6].toString());
                row.getCell(6).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(7).setCellValue(data[r][7].toString());
                row.getCell(7).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(8).setCellValue(data[r][8].toString());
                row.getCell(8).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(9).setCellValue(data[r][9].toString());
                row.getCell(9).setCellStyle(isZebra ? zebraTextStyle : textStyle);
            }

            // Total row
            int sumRowIdx = data.length + 4;
            Row sumRow = sheet.createRow(sumRowIdx);
            sumRow.setHeightInPoints(26);
            for (int col = 0; col < 10; col++) {
                Cell sc = sumRow.createCell(col);
                sc.setCellStyle(totalRowStyle);
            }
            sumRow.getCell(0).setCellValue("TỔNG GIÁ TRỊ HỢP ĐỒNG:");
            sumRow.getCell(0).setCellStyle(totalLabelStyle);
            sheet.addMergedRegion(new CellRangeAddress(sumRowIdx, sumRowIdx, 0, 4));

            Cell sumTotal = sumRow.getCell(5);
            sumTotal.setCellFormula(String.format("SUM(F5:F%d)", sumRowIdx));
            sumTotal.setCellStyle(totalRowStyle);

            int[] widths = {1800, 3800, 13000, 9500, 3600, 6800, 3800, 3800, 5200, 11000};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 4);
            wb.write(out);
        }
    }

    /* =========================================================================
       6. MẪU XUẤT HỒ SƠ KHÁCH HÀNG (CUSTOMERS EXPORT REPORT)
       ========================================================================= */
    public static void buildCustomersExportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Khách Hàng");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createBannerTitleStyle(wb);
            CellStyle metaStyle = createMetaStyle(wb);
            CellStyle colHeaderStyle = createHeaderStyle(wb, COLOR_NAVY_DARK);
            CellStyle moneyStyle = createMoneyStyle(wb, false);
            CellStyle zebraMoneyStyle = createMoneyStyle(wb, true);
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);

            // Banner Title
            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell cTitle = r0.createCell(0);
            cTitle.setCellValue("NOVACRM ENTERPRISE - BÁO CÁO DANH SÁCH KHÁCH HÀNG DOANH NGHIỆP");
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(20);
            Cell cMeta = r1.createCell(0);
            cMeta.setCellValue("Phân loại: Khách hàng B2B | Xuất bởi: NovaCRM System | Đơn vị tiền tệ: VNĐ");
            cMeta.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

            sheet.createRow(2).setHeightInPoints(8);

            String[] headers = {
                "STT", "Mã KH", "Tên Doanh Nghiệp", "Mã Số Thuế", "Ngành Nghề",
                "Quy Mô", "Email Liên Hệ", "Số Điện Thoại", "Địa Chỉ Trụ Sở", "Trạng Thái", "Doanh Thu Đã Ký (VNĐ)"
            };
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            Object[][] data = {
                {1, "KH-0001", "Công ty TNHH Công Nghệ Ánh Dương", "0108923456", "Công nghệ thông tin", "50 - 200 nhân sự", "contact@anhduong.vn", "02438889999", "Tầng 8 Keangnam, Mễ Trì, Hà Nội", "CHÍNH THỨC", 250000000.0},
                {2, "KH-0002", "Tập đoàn Bán lẻ Sao Việt", "0312456789", "Bán lẻ / Thương mại", "Trên 500 nhân sự", "info@saoviet.com.vn", "02839998888", "123 Nguyễn Thị Minh Khai, Q1, TP.HCM", "ĐANG GIAO DỊCH", 45000000.0},
                {3, "KH-0003", "Công ty Cổ phần Giải pháp Số Nam Á", "0401889922", "Công nghệ thông tin", "10 - 50 nhân sự", "admin@nama.vn", "0988776655", "45 Lê Duẩn, Hải Châu, Đà Nẵng", "TIỀM NĂNG", 68000000.0},
                {4, "KH-0004", "Công ty Cổ phần Công nghệ Mới", "0109988776", "Công nghệ thông tin", "10 - 50 nhân sự", "contact@newtech.vn", "0901234567", "Tòa nhà FPT, Cầu Giấy, Hà Nội", "CHÍNH THỨC", 18905000.0}
            };

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 4);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);

                row.createCell(0).setCellValue((Integer) data[r][0]);
                row.getCell(0).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(1).setCellValue(data[r][1].toString());
                row.getCell(1).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(2).setCellValue(data[r][2].toString());
                row.getCell(2).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(3).setCellValue(data[r][3].toString());
                row.getCell(3).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(4).setCellValue(data[r][4].toString());
                row.getCell(4).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(5).setCellValue(data[r][5].toString());
                row.getCell(5).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(6).setCellValue(data[r][6].toString());
                row.getCell(6).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(7).setCellValue(data[r][7].toString());
                row.getCell(7).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(8).setCellValue(data[r][8].toString());
                row.getCell(8).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(9).setCellValue(data[r][9].toString());
                row.getCell(9).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                Cell cVal = row.createCell(10);
                cVal.setCellValue((Double) data[r][10]);
                cVal.setCellStyle(isZebra ? zebraMoneyStyle : moneyStyle);
            }

            int[] widths = {1800, 3200, 11000, 4200, 6000, 5200, 7500, 4500, 11000, 4800, 6500};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 4);
            wb.write(out);
        }
    }

    /* =========================================================================
       7. MẪU XUẤT NGƯỜI LIÊN HỆ (CONTACTS EXPORT REPORT)
       ========================================================================= */
    public static void buildContactsExportTemplate(OutputStream out) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            XSSFSheet sheet = wb.createSheet("Người Liên Hệ");
            sheet.setDisplayGridlines(true);

            CellStyle titleStyle = createBannerTitleStyle(wb);
            CellStyle metaStyle = createMetaStyle(wb);
            CellStyle colHeaderStyle = createHeaderStyle(wb, new byte[]{(byte) 16, (byte) 185, (byte) 129}); // Emerald green
            CellStyle textStyle = createDataStyle(wb, HorizontalAlignment.LEFT, false);
            CellStyle zebraTextStyle = createDataStyle(wb, HorizontalAlignment.LEFT, true);
            CellStyle centerStyle = createDataStyle(wb, HorizontalAlignment.CENTER, false);
            CellStyle zebraCenterStyle = createDataStyle(wb, HorizontalAlignment.CENTER, true);

            Row r0 = sheet.createRow(0);
            r0.setHeightInPoints(34);
            Cell cTitle = r0.createCell(0);
            cTitle.setCellValue("NOVACRM ENTERPRISE - DANH BẠ NGƯỜI LIÊN HỆ & ĐẦU MỐI DOANH NGHIỆP");
            cTitle.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 8));

            Row r1 = sheet.createRow(1);
            r1.setHeightInPoints(20);
            Cell cMeta = r1.createCell(0);
            cMeta.setCellValue("Phân loại: Contacts & Key Stakeholders | Xuất bởi: NovaCRM System");
            cMeta.setCellStyle(metaStyle);
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 8));

            sheet.createRow(2).setHeightInPoints(8);

            String[] headers = {
                "STT", "Mã LH", "Họ và Tên", "Doanh Nghiệp Trực Thuộc", "Chức Danh",
                "Email Công Việc", "Số Di Động", "Vai Trò Ra Quyết Định", "Đầu Mối Chính"
            };
            Row r3 = sheet.createRow(3);
            r3.setHeightInPoints(26);
            for (int i = 0; i < headers.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(colHeaderStyle);
            }

            Object[][] data = {
                {1, "CT-001", "Nguyễn Hoàng Nam", "Công ty TNHH Công Nghệ Ánh Dương", "Giám đốc Công nghệ (CTO)", "nam.nh@anhduong.vn", "0901234567", "Người quyết định cuối cùng (Decision Maker)", "CÓ"},
                {2, "CT-002", "Trần Thu Trang", "Tập đoàn Bán lẻ Sao Việt", "Trưởng phòng Mua sắm (Procurement)", "trang.tt@saoviet.com.vn", "0912345678", "Người đánh giá kỹ thuật (Evaluator)", "CÓ"},
                {3, "CT-003", "Lê Văn Hùng", "Công ty CP Giải pháp Số Nam Á", "Giám đốc Điều hành (CEO)", "hung.lv@nama.vn", "0933557799", "Chủ tài trợ dự án (Sponsor)", "CÓ"},
                {4, "CT-004", "Phạm Thị Lan", "Công ty Cổ phần Công nghệ Mới", "Kế toán trưởng (Chief Accountant)", "lan.pt@newtech.vn", "0988664422", "Ký duyệt hợp đồng & Thanh toán", "KHÔNG"}
            };

            for (int r = 0; r < data.length; r++) {
                Row row = sheet.createRow(r + 4);
                row.setHeightInPoints(22);
                boolean isZebra = (r % 2 == 1);

                row.createCell(0).setCellValue((Integer) data[r][0]);
                row.getCell(0).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(1).setCellValue(data[r][1].toString());
                row.getCell(1).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(2).setCellValue(data[r][2].toString());
                row.getCell(2).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(3).setCellValue(data[r][3].toString());
                row.getCell(3).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(4).setCellValue(data[r][4].toString());
                row.getCell(4).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(5).setCellValue(data[r][5].toString());
                row.getCell(5).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(6).setCellValue(data[r][6].toString());
                row.getCell(6).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);

                row.createCell(7).setCellValue(data[r][7].toString());
                row.getCell(7).setCellStyle(isZebra ? zebraTextStyle : textStyle);

                row.createCell(8).setCellValue(data[r][8].toString());
                row.getCell(8).setCellStyle(isZebra ? zebraCenterStyle : centerStyle);
            }

            int[] widths = {1800, 3200, 6800, 11000, 7500, 7500, 4500, 11000, 3800};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i]);
            }

            sheet.createFreezePane(0, 4);
            wb.write(out);
        }
    }

    private static CellStyle createBannerTitleStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(new XSSFColor(new byte[]{(byte) 30, (byte) 58, (byte) 138}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createMetaStyle(XSSFWorkbook wb) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        font.setColor(new XSSFColor(new byte[]{(byte) 100, (byte) 116, (byte) 139}, null));
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.LEFT);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        return style;
    }

    private static CellStyle createHeaderStyle(XSSFWorkbook wb, byte[] rgbColor) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 10.5);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(rgbColor, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorders(style, BorderStyle.THIN, new XSSFColor(COLOR_BORDER_GRAY, null));
        return style;
    }

    private static CellStyle createDataStyle(XSSFWorkbook wb, HorizontalAlignment align, boolean zebra) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setFontHeightInPoints((short) 10);
        style.setFont(font);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        if (zebra) {
            style.setFillForegroundColor(new XSSFColor(COLOR_ZEBRA_LIGHT, null));
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        setBorders(style, BorderStyle.THIN, new XSSFColor(COLOR_BORDER_GRAY, null));
        return style;
    }

    private static CellStyle createMoneyStyle(XSSFWorkbook wb, boolean zebra) {
        XSSFCellStyle style = (XSSFCellStyle) createDataStyle(wb, HorizontalAlignment.RIGHT, zebra);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0 \"₫\""));
        return style;
    }

    private static CellStyle createTotalRowStyle(XSSFWorkbook wb, HorizontalAlignment align) {
        XSSFCellStyle style = wb.createCellStyle();
        XSSFFont font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(new XSSFColor(new byte[]{(byte) 30, (byte) 58, (byte) 138}, null));
        style.setFont(font);
        style.setFillForegroundColor(new XSSFColor(COLOR_TOTAL_BG, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(align);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0 \"₫\""));

        XSSFColor borderCol = new XSSFColor(new byte[]{(byte) 99, (byte) 102, (byte) 241}, null);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.DOUBLE);
        style.setTopBorderColor(borderCol);
        style.setBottomBorderColor(borderCol);
        return style;
    }

    private static void setBorders(XSSFCellStyle style, BorderStyle bStyle, XSSFColor color) {
        style.setBorderTop(bStyle);
        style.setBorderBottom(bStyle);
        style.setBorderLeft(bStyle);
        style.setBorderRight(bStyle);
        style.setTopBorderColor(color);
        style.setBottomBorderColor(color);
        style.setLeftBorderColor(color);
        style.setRightBorderColor(color);
    }

    /**
     * Phương thức chạy trực tiếp để tạo bộ file mẫu mẫu vào thư mục tĩnh trên đĩa
     */
    public static void generateAllStaticFiles(String targetDir) throws IOException {
        File dir = new File(targetDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 1. Mẫu Nhập
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Nhap_Khach_Hang_NovaCRM.xlsx"))) {
            buildCustomerImportTemplate(fos);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Nhap_Lead_Tiem_Nang_NovaCRM.xlsx"))) {
            buildLeadImportTemplate(fos);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Nhap_Nguoi_Dung_NovaCRM.xlsx"))) {
            buildUserImportTemplate(fos);
        }

        // 2. Mẫu Xuất
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Xuat_Bao_Gia_NovaCRM.xlsx"))) {
            buildQuotationsExportTemplate(fos);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Xuat_Hop_Dong_NovaCRM.xlsx"))) {
            buildContractsExportTemplate(fos);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Xuat_Khach_Hang_NovaCRM.xlsx"))) {
            buildCustomersExportTemplate(fos);
        }
        try (FileOutputStream fos = new FileOutputStream(new File(dir, "Mau_Xuat_Nguoi_Lien_He_NovaCRM.xlsx"))) {
            buildContactsExportTemplate(fos);
        }
    }

    public static void main(String[] args) throws Exception {
        String target = args.length > 0 ? args[0] : "frontend/assets/templates/excel";
        generateAllStaticFiles(target);
        System.out.println("Tất cả các tệp mẫu Excel đã được tạo thành công tại: " + target);
    }
}
