package com.crm.service.excel;

import com.crm.dao.excel.UserImportDAO;
import com.crm.dto.excel.ImportReportResult;
import com.crm.dto.excel.ImportRowData;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CRM-32 ExcelService:
 * 1. Template generation (.xlsx / .csv)
 * 2. Row parsing and business validation
 * 3. Skipping invalid rows & saving valid rows
 * 4. Reporting results
 */
@ExtendWith(MockitoExtension.class)
class ExcelServiceTest {

    @Mock
    private UserImportDAO userImportDAO;

    private ExcelService excelService;

    @BeforeEach
    void setUp() {
        excelService = new ExcelService(userImportDAO);
    }

    @Nested
    @DisplayName("Template Generation Tests")
    class TemplateGenerationTests {

        @Test
        @DisplayName("generateTemplate('xlsx') produces valid Excel workbook")
        void generateXlsxTemplate_validWorkbook() throws IOException {
            byte[] bytes = excelService.generateTemplate("xlsx");
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);

            try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
                assertNotNull(workbook.getSheet("Danh sách người dùng"));
                assertEquals("Họ và tên (*)", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
                assertEquals("Địa chỉ Email (*)", workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue());
            }
        }

        @Test
        @DisplayName("generateTemplate('csv') produces valid CSV with UTF-8 BOM")
        void generateCsvTemplate_validCsv() throws IOException {
            byte[] bytes = excelService.generateTemplate("csv");
            assertNotNull(bytes);
            assertTrue(bytes.length > 0);

            String content = new String(bytes, StandardCharsets.UTF_8);
            assertTrue(content.contains("Họ và tên (*)"));
            assertTrue(content.contains("Địa chỉ Email (*)"));
        }
    }

    @Nested
    @DisplayName("Row Validation & Parsing Tests")
    class RowValidationTests {

        @BeforeEach
        void mockDaoLookups() throws SQLException {
            lenient().when(userImportDAO.findExistingEmails(any(), anyCollection()))
                    .thenReturn(Set.of("existing@crm.vn"));
            lenient().when(userImportDAO.findExistingUsernames(any(), anyCollection()))
                    .thenReturn(Set.of("existing_user"));
            lenient().when(userImportDAO.findAllRolesMap(any()))
                    .thenReturn(Map.of("sales rep", 1L, "team lead", 2L, "admin", 3L));
            lenient().when(userImportDAO.findAllTeamsMap(any()))
                    .thenReturn(Map.of("miền bắc", 10L, "miền nam", 20L));
        }

        @Test
        @DisplayName("Valid CSV rows are classified as valid")
        void validRows_classifiedAsValid() throws Exception {
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + "Nguyen Van A,nguyenvana@test.com,vana,0912345678,Sales Rep,Miền Bắc\n"
                    + "Tran Thi B,tranthib@test.com,binhtt,0987654321,Team Lead,Miền Bắc\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult result = excelService.parseAndValidate(inputStream, "test.csv");

            assertNotNull(result);
            assertEquals(2, result.getTotalRows());
            assertEquals(2, result.getValidRows().size());
            assertEquals(0, result.getErrorRows().size());
            assertNotNull(result.getBatchToken());
        }

        @Test
        @DisplayName("Missing full name is flagged as error")
        void missingFullName_flaggedAsError() throws Exception {
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + ",nguyenvana@test.com,vana,0912345678,Sales Rep,Miền Bắc\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult result = excelService.parseAndValidate(inputStream, "test.csv");

            assertEquals(1, result.getTotalRows());
            assertEquals(0, result.getValidRows().size());
            assertEquals(1, result.getErrorRows().size());
            assertTrue(result.getErrorRows().get(0).getErrors().stream().anyMatch(e -> e.contains("Họ và tên")));
        }

        @Test
        @DisplayName("Invalid email format is flagged as error")
        void invalidEmail_flaggedAsError() throws Exception {
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + "Nguyen Van A,not-an-email,vana,0912345678,Sales Rep,Miền Bắc\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult result = excelService.parseAndValidate(inputStream, "test.csv");

            assertEquals(1, result.getTotalRows());
            assertEquals(0, result.getValidRows().size());
            assertEquals(1, result.getErrorRows().size());
            assertTrue(result.getErrorRows().get(0).getErrors().stream().anyMatch(e -> e.contains("email không hợp lệ")));
        }

        @Test
        @DisplayName("Email already existing in Database is flagged as error")
        void existingDbEmail_flaggedAsError() throws Exception {
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + "Nguyen Van A,existing@crm.vn,vana,0912345678,Sales Rep,Miền Bắc\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult result = excelService.parseAndValidate(inputStream, "test.csv");

            assertEquals(1, result.getTotalRows());
            assertEquals(0, result.getValidRows().size());
            assertEquals(1, result.getErrorRows().size());
            assertTrue(result.getErrorRows().get(0).getErrors().stream().anyMatch(e -> e.contains("đã tồn tại")));
        }

        @Test
        @DisplayName("Duplicate email within the same file is flagged on second occurrence")
        void duplicateEmailInFile_flaggedAsError() throws Exception {
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + "Nguyen Van A,dup@test.com,user1,0912345678,Sales Rep,Miền Bắc\n"
                    + "Nguyen Van B,dup@test.com,user2,0987654321,Sales Rep,Miền Bắc\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult result = excelService.parseAndValidate(inputStream, "test.csv");

            assertEquals(2, result.getTotalRows());
            assertEquals(1, result.getValidRows().size());
            assertEquals(1, result.getErrorRows().size());
            assertTrue(result.getErrorRows().get(0).getErrors().stream().anyMatch(e -> e.contains("trùng lặp với dòng khác")));
        }

        @Test
        @DisplayName("Team Lead without a team is flagged with Team Lead validation error (CRM-29 Rule 2)")
        void teamLeadWithoutTeam_flaggedAsError() throws Exception {
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + "Nguyen Van A,lead@test.com,user1,0912345678,Team Lead,\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult result = excelService.parseAndValidate(inputStream, "test.csv");

            assertEquals(1, result.getTotalRows());
            assertEquals(0, result.getValidRows().size());
            assertEquals(1, result.getErrorRows().size());
            assertTrue(result.getErrorRows().get(0).getErrors().stream().anyMatch(e -> e.contains("Team Lead")));
        }
    }

    @Nested
    @DisplayName("Import Execution Tests")
    class ImportExecutionTests {

        @Test
        @DisplayName("confirmImport executes persistence only for valid rows and skips error rows")
        void confirmImport_savesValidRowsAndSkipsErrors() throws Exception {
            when(userImportDAO.findExistingEmails(any(), anyCollection())).thenReturn(Set.of());
            when(userImportDAO.findExistingUsernames(any(), anyCollection())).thenReturn(Set.of());
            when(userImportDAO.findAllRolesMap(any())).thenReturn(Map.of("sales rep", 1L));
            when(userImportDAO.findAllTeamsMap(any())).thenReturn(Map.of());
            when(userImportDAO.saveImportedUsers(any(), anyList(), anyString())).thenReturn(1);

            // Row 1 is valid, Row 2 is invalid (missing email)
            String csvData = "Họ và tên,Email,Tên đăng nhập,Số điện thoại,Vai trò,Nhóm kinh doanh\n"
                    + "Nguyen Van A,valid@test.com,vana,0912345678,Sales Rep,\n"
                    + "Invalid User,,invalid,0987654321,Sales Rep,\n";

            ByteArrayInputStream inputStream = new ByteArrayInputStream(csvData.getBytes(StandardCharsets.UTF_8));
            ImportReportResult previewResult = excelService.parseAndValidate(inputStream, "test.csv");

            assertEquals(2, previewResult.getTotalRows());
            assertEquals(1, previewResult.getValidRows().size());
            assertEquals(1, previewResult.getErrorRows().size());

            // Confirm step
            ImportReportResult finalReport = excelService.confirmImport(previewResult.getBatchToken());

            assertNotNull(finalReport);
            assertEquals(2, finalReport.getTotalRows());
            assertEquals(1, finalReport.getSuccessRows());
            assertEquals(1, finalReport.getFailedRows());

            // Verify saveImportedUsers was called with only the 1 valid row
            verify(userImportDAO, times(1)).saveImportedUsers(any(), argThat(list -> list.size() == 1), anyString());
        }

        @Test
        @DisplayName("confirmImport with non-existent token throws IllegalStateException")
        void confirmImport_invalidToken_throws() {
            assertThrows(IllegalStateException.class, () -> excelService.confirmImport("non_existent_token"));
        }
    }
}
