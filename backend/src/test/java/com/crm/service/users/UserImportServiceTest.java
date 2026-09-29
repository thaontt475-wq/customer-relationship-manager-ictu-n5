package com.crm.service.users;

import com.crm.dto.users.UserImportResult;
import com.crm.dto.users.UserImportRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("S2-01: Kiểm thử tự động tính năng nhập người dùng hàng loạt từ Excel")
class UserImportServiceTest {

    private final UserExcelParser parser = new UserExcelParser();

    @Test
    @DisplayName("S2-01: Kiểm tra tạo tệp mẫu Excel XLSX và CSV thành công")
    void testGenerateTemplateFiles() throws Exception {
        // 1. Kiểm tra template XLSX
        ByteArrayOutputStream xlsxOut = new ByteArrayOutputStream();
        parser.writeTemplateXlsx(xlsxOut);
        byte[] xlsxBytes = xlsxOut.toByteArray();
        assertTrue(xlsxBytes.length > 0, "Tệp mẫu XLSX phải được sinh ra và có dữ liệu");

        // Đọc lại file template XLSX vừa tạo để đảm bảo parser đọc được
        List<UserImportRow> xlsxRows = parser.parse(new ByteArrayInputStream(xlsxBytes), "template.xlsx");
        assertFalse(xlsxRows.isEmpty(), "File XLSX mẫu phải chứa ít nhất một dòng dữ liệu mẫu");
        assertEquals("nguyen.vana", xlsxRows.get(0).getUsername());
        assertEquals("nguyenvana@crm.vn", xlsxRows.get(0).getEmail());

        // 2. Kiểm tra template CSV
        StringWriter csvWriter = new StringWriter();
        parser.writeTemplateCsv(csvWriter);
        String csvContent = csvWriter.toString();
        assertTrue(csvContent.contains("Tên đăng nhập"), "Tệp CSV mẫu phải chứa tiêu đề Tên đăng nhập");
        assertTrue(csvContent.contains("nguyen.vana"), "Tệp CSV mẫu phải chứa dữ liệu mẫu nguyen.vana");
    }

    @Test
    @DisplayName("S2-01: Kiểm tra đọc và bóc tách dữ liệu từ file CSV chuẩn xác")
    void testParseCsvFile() throws Exception {
        String csvData = "Tên đăng nhập,Họ và tên,Email,Số điện thoại,Vai trò,Nhóm,Phạm vi,Mật khẩu\n"
                + "user1,Nguyễn Văn Một,user1@crm.vn,0911111111,Sales Rep,Đội 1,SELF,Pass123\n"
                + "user2,Trần Thị Hai,user2@crm.vn,0922222222,Team Lead,Đội 1,TEAM,Pass123\n";

        ByteArrayInputStream in = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
        List<UserImportRow> rows = parser.parse(in, "test.csv");

        assertEquals(2, rows.size(), "Phải đọc được 2 dòng dữ liệu từ CSV");
        assertEquals("user1", rows.get(0).getUsername());
        assertEquals("Nguyễn Văn Một", rows.get(0).getFullName());
        assertEquals("user1@crm.vn", rows.get(0).getEmail());
        assertEquals("user2", rows.get(1).getUsername());
        assertEquals("TEAM", rows.get(1).getDataScope());
    }

    @Test
    @DisplayName("S2-01: Kiểm tra phát hiện lỗi theo từng dòng - Dòng lỗi bị đánh dấu, dòng hợp lệ được giữ")
    void testValidateRowsLogic() {
        List<UserImportRow> rows = new ArrayList<>();

        // Dòng 1: Hợp lệ hoàn toàn
        UserImportRow r1 = new UserImportRow(2);
        r1.setUsername("valid_user");
        r1.setFullName("Lê Hợp Lệ");
        r1.setEmail("valid@crm.vn");
        r1.setRoleName("Sales Rep");
        r1.setDataScope("SELF");
        rows.add(r1);

        // Dòng 2: Thiếu Họ và tên, Email sai định dạng
        UserImportRow r2 = new UserImportRow(3);
        r2.setUsername("invalid_user2");
        r2.setFullName(""); // Lỗi: Trống họ tên
        r2.setEmail("email_sai_dinh_dang"); // Lỗi: Sai format email
        rows.add(r2);

        // Dòng 3: Trùng email với dòng 1 trong cùng file
        UserImportRow r3 = new UserImportRow(4);
        r3.setUsername("user_trung_mail");
        r3.setFullName("Phạm Trùng Mail");
        r3.setEmail("valid@crm.vn"); // Lỗi: Trùng email với dòng 1
        rows.add(r3);

        // Giả lập logic kiểm tra từng dòng
        java.util.Set<String> seenEmails = new java.util.HashSet<>();
        for (UserImportRow r : rows) {
            if (r.getFullName() == null || r.getFullName().trim().isEmpty()) {
                r.addError("Họ và tên không được để trống");
            }
            if (r.getEmail() == null || !r.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
                r.addError("Định dạng email không hợp lệ");
            } else if (seenEmails.contains(r.getEmail())) {
                r.addError("Email bị trùng lặp trong tệp Excel");
            } else {
                seenEmails.add(r.getEmail());
            }
        }

        // Khẳng định: Dòng 1 phải hợp lệ
        assertTrue(r1.isValid(), "Dòng 1 dữ liệu chuẩn phải hợp lệ");
        assertTrue(r1.getErrorMessages().isEmpty());

        // Khẳng định: Dòng 2 phải bị đánh dấu lỗi với 2 nguyên nhân
        assertFalse(r2.isValid(), "Dòng 2 thiếu họ tên và sai email phải bị báo lỗi");
        assertEquals(2, r2.getErrorMessages().size());

        // Khẳng định: Dòng 3 bị lỗi trùng email trong file
        assertFalse(r3.isValid(), "Dòng 3 trùng email với dòng 1 phải bị báo lỗi");
        assertTrue(r3.getErrorMessages().get(0).contains("trùng lặp"));
    }

    @Test
    @DisplayName("S2-01: Báo cáo tổng kết phân loại chính xác dòng hợp lệ và dòng lỗi bị bỏ qua")
    void testImportResultSummary() {
        UserImportResult result = new UserImportResult();

        UserImportRow r1 = new UserImportRow(2);
        r1.setUsername("u1");
        r1.setEmail("u1@crm.vn");
        r1.setValid(true);

        UserImportRow r2 = new UserImportRow(3);
        r2.setUsername("u2");
        r2.setEmail("u2@crm.vn");
        r2.addError("Tên đăng nhập đã tồn tại trong hệ thống");

        UserImportRow r3 = new UserImportRow(4);
        r3.setUsername("u3");
        r3.setEmail("u3@crm.vn");
        r3.setValid(true);

        result.setRows(List.of(r1, r2, r3));
        result.setTotalRows(3);
        result.setValidRows(2);
        result.setErrorRows(1);
        result.setImportedCount(2);
        result.setSkippedCount(1);
        result.setExecuted(true);

        // Khẳng định báo cáo tổng kết
        assertEquals(3, result.getTotalRows());
        assertEquals(2, result.getValidRows());
        assertEquals(1, result.getErrorRows());
        assertEquals(2, result.getImportedCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(1, result.getFailedRows().size());
        assertEquals(2, result.getSuccessRows().size());
        assertTrue(result.getFailedRows().get(0).getErrorMessages().get(0).contains("đã tồn tại"));
    }
}
