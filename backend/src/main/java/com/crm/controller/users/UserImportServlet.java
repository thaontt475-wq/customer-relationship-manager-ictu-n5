package com.crm.controller.users;

import com.crm.dto.permissions.MenuItem;
import com.crm.dto.users.UserImportResult;
import com.crm.service.permissions.MenuService;
import com.crm.service.users.UserImportService;
import com.crm.util.SessionKey;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

@WebServlet({
        "/users/import",
        "/users/import/template",
        "/users/import/preview",
        "/users/import/execute"
})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1MB
        maxFileSize = 10 * 1024 * 1024,       // 10MB
        maxRequestSize = 20 * 1024 * 1024     // 20MB
)
public class UserImportServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(UserImportServlet.class.getName());
    private static final String USER_IMPORT_JSP = "/jsp/users/user-import.jsp";
    private static final String SESSION_PREVIEW_KEY = "USER_IMPORT_PREVIEW_DATA";

    private final UserImportService userImportService = new UserImportService();
    private final MenuService menuService = new MenuService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (!checkAdminPermission(request, response)) {
            return;
        }

        String servletPath = request.getServletPath();

        if ("/users/import/template".equals(servletPath)) {
            handleDownloadTemplate(request, response);
            return;
        }

        // Tải trang nhập người dùng
        if ("1".equals(request.getParameter("clear"))) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_PREVIEW_KEY);
            }
        }

        populateMenu(request);
        request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding(StandardCharsets.UTF_8.name());

        if (!checkAdminPermission(request, response)) {
            return;
        }

        String servletPath = request.getServletPath();

        try {
            if ("/users/import/preview".equals(servletPath)) {
                handlePreview(request, response);
                return;
            }

            if ("/users/import/execute".equals(servletPath)) {
                handleExecute(request, response);
                return;
            }

            response.sendError(HttpServletResponse.SC_NOT_FOUND);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Lỗi cơ sở dữ liệu khi nhập người dùng", e);
            request.setAttribute("errorMessage", "Lỗi cơ sở dữ liệu: " + e.getMessage());
            populateMenu(request);
            request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
        }
    }

    private void handlePreview(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        Part filePart = request.getPart("file");
        if (filePart == null || filePart.getSize() == 0) {
            request.setAttribute("errorMessage", "Vui lòng chọn tệp Excel (.xlsx hoặc .csv) để nhập");
            populateMenu(request);
            request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
            return;
        }

        String submittedFileName = filePart.getSubmittedFileName();
        if (submittedFileName == null || (!submittedFileName.toLowerCase().endsWith(".xlsx")
                && !submittedFileName.toLowerCase().endsWith(".xls")
                && !submittedFileName.toLowerCase().endsWith(".csv"))) {
            request.setAttribute("errorMessage", "Định dạng tệp không được hỗ trợ. Vui lòng tải lên tệp .xlsx hoặc .csv");
            populateMenu(request);
            request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
            return;
        }

        UserImportResult result = userImportService.preview(filePart.getInputStream(), submittedFileName);

        HttpSession session = request.getSession(true);
        session.setAttribute(SESSION_PREVIEW_KEY, result);

        request.setAttribute("previewResult", result);
        request.setAttribute("fileName", submittedFileName);
        populateMenu(request);
        request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
    }

    private void handleExecute(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException {

        HttpSession session = request.getSession(false);
        UserImportResult previewResult = session != null ? (UserImportResult) session.getAttribute(SESSION_PREVIEW_KEY) : null;

        if (previewResult == null) {
            response.sendRedirect(request.getContextPath() + "/users/import");
            return;
        }

        UserImportResult report = userImportService.executeImport(previewResult);

        if (session != null) {
            session.removeAttribute(SESSION_PREVIEW_KEY);
        }

        request.setAttribute("importReport", report);
        populateMenu(request);
        request.getRequestDispatcher(USER_IMPORT_JSP).forward(request, response);
    }

    private void handleDownloadTemplate(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String format = request.getParameter("format");

        if ("csv".equalsIgnoreCase(format)) {
            response.setContentType("text/csv; charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung.csv\"");
            PrintWriter writer = response.getWriter();
            userImportService.generateTemplateCsv(writer);
        } else {
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=\"mau_nhap_nguoi_dung.xlsx\"");
            OutputStream out = response.getOutputStream();
            userImportService.generateTemplateXlsx(out);
            out.flush();
        }
    }

    private boolean checkAdminPermission(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }

        Object rolesValue = session.getAttribute(SessionKey.ROLES);
        if (!(rolesValue instanceof Collection<?> roles)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện chức năng này.");
            return false;
        }

        boolean allowed = roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(role -> role.trim().toLowerCase(Locale.ROOT))
                .anyMatch(role -> "admin".equals(role) || "director".equals(role));

        if (!allowed) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Chỉ Quản trị viên (Admin) hoặc Giám đốc mới có quyền nhập người dùng hàng loạt.");
            return false;
        }

        return true;
    }

    private void populateMenu(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object rolesObj = session.getAttribute(SessionKey.ROLES);
            if (rolesObj instanceof Collection<?> roles) {
                List<String> roleStrings = roles.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
                List<MenuItem> menuItems = menuService.getMenuItems(roleStrings);
                request.setAttribute("menuItems", menuItems);
            }
        }
    }
}
