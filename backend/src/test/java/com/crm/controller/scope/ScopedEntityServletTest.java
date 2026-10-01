package com.crm.controller.scope;

import com.crm.service.scope.*;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.*;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import java.io.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ScopedEntityServletTest {
    @ParameterizedTest
    @EnumSource(ScopeEntityType.class)
    void exportsScopedSearchAsRealExcelUsingOnlyBinaryStream(ScopeEntityType type) throws Exception {
        var service = mock(DataScopeService.class);
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        var session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(10L);
        when(request.getServletPath()).thenReturn("/api/" + type.tableName());
        when(request.getPathInfo()).thenReturn("/export");
        when(request.getParameter("q")).thenReturn("khách");
        when(service.list(10L, type, "khách")).thenReturn(List.of(
                new ScopeRecord(9007199254740993L, "=HYPERLINK(\"https://example.invalid\")", 10L, null)));
        var bytes = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new ServletOutputStream() {
            public boolean isReady() { return true; }
            public void setWriteListener(WriteListener listener) { }
            public void write(int value) { bytes.write(value); }
        });
        new ScopedEntityServlet(service).doGet(request, response);
        verify(service).list(10L, type, "khách");
        verify(response, never()).getWriter();
        verify(response).setStatus(200);
        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals(2, sheet.getPhysicalNumberOfRows());
            assertEquals("9007199254740993", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals(CellType.STRING, sheet.getRow(1).getCell(1).getCellType());
            assertEquals("10", sheet.getRow(1).getCell(2).getStringCellValue());
        }
    }

    @ParameterizedTest
    @EnumSource(ScopeEntityType.class)
    void anonymousExportNeverQueriesData(ScopeEntityType type) throws Exception {
        var service = mock(DataScopeService.class);
        var request = mock(HttpServletRequest.class);
        var response = mock(HttpServletResponse.class);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
        new ScopedEntityServlet(service).doGet(request, response);
        verify(response).setStatus(401);
        verifyNoInteractions(service);
        verify(response, never()).getOutputStream();
    }
}
