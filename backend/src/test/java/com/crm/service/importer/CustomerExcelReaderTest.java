package com.crm.service.importer;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CustomerExcelReaderTest {
    private final CustomerExcelReader reader=new CustomerExcelReader();
    private byte[] workbook(List<String> headers,List<List<String>> rows) throws Exception {
        try(XSSFWorkbook book=new XSSFWorkbook()) {
            Sheet sheet=book.createSheet("Customers");
            Row header=sheet.createRow(0);
            for(int i=0;i<headers.size();i++) header.createCell(i).setCellValue(headers.get(i));
            for(int r=0;r<rows.size();r++) {
                Row row=sheet.createRow(r+1);
                for(int c=0;c<rows.get(r).size();c++) row.createCell(c).setCellValue(rows.get(r).get(c));
            }
            var out=new ByteArrayOutputStream();book.write(out);return out.toByteArray();
        }
    }
    private List<String> valid(String name,String tax) {
        return List.of(name,tax,"","contact@example.invalid","0123456789","https://example.invalid","Address","","");
    }
    @Test void templateOpensAsXlsxWithNoSeededCustomers() throws Exception {
        var output=new ByteArrayOutputStream();reader.writeTemplate(output);
        try(XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(output.toByteArray()))) {
            assertEquals(1,book.getNumberOfSheets());
            assertEquals(0,book.getSheetAt(0).getLastRowNum());
            for(int i=0;i<CustomerExcelReader.COLUMNS.size();i++) assertEquals(CustomerExcelReader.COLUMNS.get(i),book.getSheetAt(0).getRow(0).getCell(i).getStringCellValue());
        }
        assertTrue(reader.read(new ByteArrayInputStream(output.toByteArray())).isEmpty());
    }
    @Test void retainsAllErrorsAndRowNumbers() throws Exception {
        var rows=reader.read(new ByteArrayInputStream(workbook(CustomerExcelReader.COLUMNS,List.of(
            List.of("","","BAD","invalid","abc","ftp://host","","-1","1.5"),
            valid("Customer","0123456789")))));
        assertEquals(2,rows.size());assertEquals(2,rows.getFirst().row());
        assertTrue(rows.getFirst().errors().size()>=7);
        assertTrue(rows.get(1).errors().isEmpty());
        assertEquals("TIEM_NANG",rows.get(1).values().get("status"));
        assertEquals("0123456789",reader.request(rows.get(1)).getTaxCode());
    }
    @Test void duplicateFileRowsAreAllRejected() throws Exception {
        var rows=reader.read(new ByteArrayInputStream(workbook(CustomerExcelReader.COLUMNS,List.of(
            valid("A","0123456789"),valid("B","0123456789")))));
        assertTrue(rows.stream().allMatch(r->r.errors().stream().anyMatch(e->e.contains("trùng trong file"))));
    }
    @Test void skipsEmptyRowsAndAllowsOptionalTaxCode() throws Exception {
        var rows=reader.read(new ByteArrayInputStream(workbook(CustomerExcelReader.COLUMNS,List.of(
            Collections.nCopies(9,""),valid("A","")))));
        assertEquals(1,rows.size());assertEquals(3,rows.getFirst().row());
        assertNull(reader.request(rows.getFirst()).getTaxCode());
    }
    @Test void acceptsReorderedHeadersButRejectsMissingAndRepeatedHeaders() throws Exception {
        var columns=new ArrayList<>(CustomerExcelReader.COLUMNS);Collections.reverse(columns);
        var values=new ArrayList<>(valid("A","0123456789"));Collections.reverse(values);
        assertEquals("A",reader.read(new ByteArrayInputStream(workbook(columns,List.of(values)))).getFirst().values().get("name"));
        assertThrows(IllegalArgumentException.class,()->reader.read(new ByteArrayInputStream(workbook(List.of("name"),List.of()))));
        columns=new ArrayList<>(CustomerExcelReader.COLUMNS);columns.add("name");
        byte[] repeated=workbook(columns,List.of());
        assertThrows(IllegalArgumentException.class,()->reader.read(new ByteArrayInputStream(repeated)));
    }
    @Test void rejectsFormulaAndNumericTaxWithoutLosingOtherRows() throws Exception {
        byte[] initial=workbook(CustomerExcelReader.COLUMNS,List.of(valid("A","0123456789"),valid("B","1123456789")));
        byte[] changed;
        try(XSSFWorkbook book=new XSSFWorkbook(new ByteArrayInputStream(initial))) {
            book.getSheetAt(0).getRow(1).getCell(0).setCellFormula("\"Customer\"");
            book.getSheetAt(0).getRow(1).getCell(1).setCellValue(1234567890);
            var output=new ByteArrayOutputStream();book.write(output);changed=output.toByteArray();
        }
        var rows=reader.read(new ByteArrayInputStream(changed));
        assertTrue(rows.getFirst().errors().stream().anyMatch(e->e.contains("công thức")));
        assertTrue(rows.getFirst().errors().stream().anyMatch(e->e.contains("Text")));
        assertTrue(rows.get(1).errors().isEmpty());
    }
    @Test void rejectsInvalidOversizedAndTooManyRows() throws Exception {
        assertThrows(IllegalArgumentException.class,()->reader.read(new ByteArrayInputStream("invalid".getBytes())));
        assertThrows(IllegalArgumentException.class,()->reader.read(new ByteArrayInputStream(new byte[CustomerExcelReader.MAX_BYTES+1])));
        byte[] rows=workbook(CustomerExcelReader.COLUMNS,Collections.nCopies(1001,valid("A","")));
        assertThrows(IllegalArgumentException.class,()->reader.read(new ByteArrayInputStream(rows)));
    }
}
