package com.crm.controller.users;

import com.crm.model.User;
import com.crm.service.teams.TeamService;
import com.crm.service.users.UserService;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserExportAuthorizationTest {
    @Test
    void exportRedirectsAnonymousRequestToLogin() throws Exception {
        TestableUserServlet servlet = new TestableUserServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getServletPath()).thenReturn("/users/export");
        when(request.getContextPath()).thenReturn("");
        when(request.getSession(false)).thenReturn(null);

        servlet.get(request, response);

        verify(response).sendRedirect("/login?expired=1");
        verify(response, never()).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void exportRejectsAuthenticatedNonAdmin() throws Exception {
        TestableUserServlet servlet = new TestableUserServlet();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getServletPath()).thenReturn("/users/export");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(52L);
        when(session.getAttribute("roles")).thenReturn(List.of("Sales Rep"));

        servlet.get(request, response);

        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
        verify(response, never()).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Test
    void exportUsesFiltersAndWritesXlsxWithBusinessColumnsOnly() throws Exception {
        UserService users = mock(UserService.class);
        User user = new User();
        user.setFullName("A User");
        user.setEmail("a@example.test");
        user.setRole("Admin");
        user.setTeamName("North Team");
        user.setDataScope("TEAM");
        user.setStatus("ACTIVE");
        when(users.searchUsers("A", "42", "Admin", "ACTIVE", 1, 100))
                .thenReturn(new UserService.UserPage(List.of(user), 1, 100, 1, 1));

        TestableUserServlet servlet = new TestableUserServlet(users, mock(TeamService.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        when(request.getServletPath()).thenReturn("/users/export");
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("userId")).thenReturn(52L);
        when(session.getAttribute("roles")).thenReturn(List.of("Admin"));
        when(request.getParameter("q")).thenReturn("A");
        when(request.getParameter("team")).thenReturn("42");
        when(request.getParameter("role")).thenReturn("Admin");
        when(request.getParameter("status")).thenReturn("ACTIVE");
        when(response.getOutputStream()).thenReturn(new ByteArrayServletOutputStream(bytes));

        servlet.get(request, response);

        verify(users).searchUsers("A", "42", "Admin", "ACTIVE", 1, 100);
        verify(response).setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        try (var workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            assertEquals("Họ tên", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Email", sheet.getRow(0).getCell(1).getStringCellValue());
            assertEquals("Phạm vi dữ liệu", sheet.getRow(0).getCell(4).getStringCellValue());
            assertEquals("A User", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("North Team", sheet.getRow(1).getCell(3).getStringCellValue());
            assertEquals("TEAM", sheet.getRow(1).getCell(4).getStringCellValue());
            assertEquals("ACTIVE", sheet.getRow(1).getCell(5).getStringCellValue());
            assertEquals(1, sheet.getLastRowNum());
            assertEquals(6, sheet.getRow(0).getLastCellNum());
        }
    }

    private static final class TestableUserServlet extends UserServlet {
        TestableUserServlet() { super(null, null); }
        TestableUserServlet(UserService users, TeamService teams) { super(users, teams); }
        void get(HttpServletRequest request, HttpServletResponse response) throws Exception { doGet(request, response); }
    }

    private static final class ByteArrayServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream output;
        ByteArrayServletOutputStream(ByteArrayOutputStream output) { this.output = output; }
        @Override public void write(int value) { output.write(value); }
        @Override public boolean isReady() { return true; }
        @Override public void setWriteListener(WriteListener listener) { }
    }
}
