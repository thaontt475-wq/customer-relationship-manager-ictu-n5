package com.crm.service.leads;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import static org.junit.jupiter.api.Assertions.*;

class LeadExcelReaderTest {
    static final List<Map<String,Object>> SOURCES=List.of(Map.of("id",10L,"code","EVENT","name","Event"));
    static byte[] file(String[][] rows) throws Exception {
        try(var book=new XSSFWorkbook()) {
            var sheet=book.createSheet("Leads");var header=sheet.createRow(0);
            for(int i=0;i<LeadExcelReader.COLUMNS.size();i++)header.createCell(i).setCellValue(LeadExcelReader.COLUMNS.get(i));
            for(int index=0;index<rows.length;index++) {
                var row=sheet.createRow(index+1);
                for(int col=0;col<rows[index].length;col++)row.createCell(col).setCellValue(rows[index][col]);
            }
            var out=new ByteArrayOutputStream();book.write(out);return out.toByteArray();
        }
    }
    private List<LeadExcelReader.ImportRow> read(byte[] bytes) throws Exception {
        return new LeadExcelReader().read(new ByteArrayInputStream(bytes),SOURCES);
    }
    @Test void realTemplateHasTextPhoneAndExactHeaders() throws Exception {
        var out=new ByteArrayOutputStream();new LeadExcelReader().template(out);
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(out.toByteArray()))) {
            assertEquals("Leads",book.getSheetName(0));
            for(int i=0;i<6;i++)assertEquals(LeadExcelReader.COLUMNS.get(i),book.getSheetAt(0).getRow(0).getCell(i).getStringCellValue());
            assertEquals("@",book.getSheetAt(0).getColumnStyle(3).getDataFormatString());
        }
    }
    @Test void validMixedRowsKeepExcelRowNumbersAndErrors() throws Exception {
        var rows=read(file(new String[][]{{"An","","an@example.invalid","0901234567","EVENT"},{"Bình","","bad","",""}}));
        assertEquals(2,rows.size());assertTrue(rows.getFirst().errors().isEmpty());assertEquals("0901234567",rows.getFirst().lead().phone());
        assertEquals(3,rows.get(1).row());assertTrue(rows.get(1).errors().stream().anyMatch(e->e.field().equals("sourceCode")));
        assertTrue(rows.get(1).errors().stream().anyMatch(e->e.field().equals("email")));
    }
    @Test void rejectsEmptyCorruptAndOversizeUploads() throws Exception {
        for(byte[] bytes:new byte[][]{new byte[0],"not excel".getBytes(),new byte[5*1024*1024+1],file(new String[0][])})
            assertThrows(IllegalArgumentException.class,()->read(bytes));
    }
    @Test void rejectsFormulaAndNumericPhoneWithoutEvaluation() throws Exception {
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(file(new String[][]{{"An","","an@example.invalid","0901234567","EVENT"}})))) {
            var row=book.getSheetAt(0).getRow(1);row.getCell(0).setCellFormula("1+1");row.getCell(3).setCellValue(901234567);
            var out=new ByteArrayOutputStream();book.write(out);var errors=read(out.toByteArray()).getFirst().errors();
            assertTrue(errors.stream().anyMatch(e->e.field().equals("fullName")&&e.message().contains("công thức")));
            assertTrue(errors.stream().anyMatch(e->e.field().equals("phone")&&e.message().contains("Text")));
        }
    }
    @Test void rejectsMacroExternalLinksAndZipExpansionBeforeParsing() throws Exception {
        for(String name:new String[]{"xl/vbaProject.bin","xl/externalLinks/externalLink1.xml","xl/big.xml"}) {
            var out=new ByteArrayOutputStream();
            try(var zip=new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry("[Content_Types].xml"));zip.write("<Types/>".getBytes());zip.closeEntry();
                zip.putNextEntry(new ZipEntry(name));zip.write(new byte[name.endsWith("big.xml")?20*1024*1024+1:1]);zip.closeEntry();
            }
            assertThrows(IllegalArgumentException.class,()->read(out.toByteArray()));
        }
    }
    @Test void rejectsExcessRowsAndBadHeaders() throws Exception {
        try(var book=new XSSFWorkbook(new ByteArrayInputStream(file(new String[][]{{"An","","an@example.invalid","","EVENT"}})))) {
            book.getSheetAt(0).createRow(1001).createCell(0).setCellValue("extra");
            var out=new ByteArrayOutputStream();book.write(out);assertThrows(IllegalArgumentException.class,()->read(out.toByteArray()));
            book.getSheetAt(0).removeRow(book.getSheetAt(0).getRow(1001));book.getSheetAt(0).getRow(0).getCell(0).setCellValue("unknown");
            out.reset();book.write(out);assertThrows(IllegalArgumentException.class,()->read(out.toByteArray()));
        }
    }
}
